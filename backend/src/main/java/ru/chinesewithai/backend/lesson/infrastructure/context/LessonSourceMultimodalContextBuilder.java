package ru.chinesewithai.backend.lesson.infrastructure.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuildRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuilder;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageContentPart;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

@Component
public class LessonSourceMultimodalContextBuilder implements AgentContextBuilder {

    public static final String KEY = "lesson-source-multimodal";

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PDF_RENDERED_PAGES = 20;

    private final ObjectMapper objectMapper;
    private final StoredFileFacade storedFiles;
    private final LessonPdfPageRenderer pdfPageRenderer;

    public LessonSourceMultimodalContextBuilder(
            ObjectMapper objectMapper, StoredFileFacade storedFiles, LessonPdfPageRenderer pdfPageRenderer) {
        this.objectMapper = objectMapper;
        this.storedFiles = storedFiles;
        this.pdfPageRenderer = pdfPageRenderer;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public List<AgentModelMessage> buildContext(AgentContextBuildRequest request) {
        var messages = new ArrayList<AgentModelMessage>();
        messages.add(AgentModelMessage.system(buildSystemPrompt(request)));

        var userParts = new ArrayList<AgentModelMessageContentPart>();
        userParts.add(AgentModelMessageContentPart.text(buildUserPrompt(request)));
        userParts.addAll(visualSourceParts(request.session().inputJson()));
        messages.add(AgentModelMessage.user(userParts));

        if (!request.profile().memoryPolicy().includePreviousSteps()) {
            return List.copyOf(messages);
        }

        var history = request.conversationHistory();
        var maxHistory = request.profile().memoryPolicy().maxStepHistoryEntries();
        var startIndex = Math.max(0, history.size() - maxHistory);
        messages.addAll(history.subList(startIndex, history.size()));
        return List.copyOf(messages);
    }

    private String buildSystemPrompt(AgentContextBuildRequest request) {
        var joiner = new StringJoiner("\n\n");
        joiner.add(request.profile().systemPrompt());
        if (request.session().systemPromptAppendix() != null) {
            joiner.add(request.session().systemPromptAppendix());
        }
        var systemSections = request.preGenerationState().contextSections().stream()
                .filter(section -> section.target() == PreGenerationContextSectionTarget.SYSTEM)
                .toList();
        if (!systemSections.isEmpty()) {
            joiner.add("Pre-generated system context:\n" + renderSections(systemSections));
        }
        joiner.add("Return the final answer as a valid JSON object without markdown fences.");
        joiner.add("Required output fields:\n" + describeOutputContract(request.profile().outputContract().requiredFields()));
        return joiner.toString();
    }

    private String buildUserPrompt(AgentContextBuildRequest request) {
        var joiner = new StringJoiner("\n\n");
        joiner.add("Task:\n" + request.session().task());
        if (request.session().inputJson() != null) {
            joiner.add("Additional input manifest:\n" + prettyPrintJson(request.session().inputJson()));
        }
        var userSections = request.preGenerationState().contextSections().stream()
                .filter(section -> section.target() == PreGenerationContextSectionTarget.USER)
                .toList();
        if (!userSections.isEmpty()) {
            joiner.add("Pre-generated user context:\n" + renderSections(userSections));
        }
        return joiner.toString();
    }

    private List<AgentModelMessageContentPart> visualSourceParts(String rawInputJson) {
        if (rawInputJson == null || rawInputJson.isBlank()) {
            return List.of();
        }
        var root = readJson(rawInputJson);
        var policy = root.path("sourceProcessingPolicy");
        if (policy.isObject() && !policy.path("attachImagesToVisionStages").asBoolean(true)) {
            return List.of();
        }
        var maxImageBytes = positiveInt(policy.path("maxInlineImageBytes"), MAX_IMAGE_BYTES);
        var maxPdfRenderedPages = positiveInt(policy.path("maxPdfRenderedPages"), MAX_PDF_RENDERED_PAGES);
        var pdfHandlingMode = policy.path("pdfHandlingMode").asText("RENDER_TO_IMAGES_WHEN_NO_TEXT");

        var bundle = root.path("sourceBundle");
        if (bundle.isMissingNode()) {
            bundle = root;
        }
        var sources = bundle.path("sources");
        if (!sources.isArray()) {
            return List.of();
        }

        var parts = new ArrayList<AgentModelMessageContentPart>();
        for (var source : sources) {
            var mediaCategory = source.path("mediaCategory").asText();
            var fileId = source.path("fileId").asText(null);
            if (fileId == null || fileId.isBlank()) {
                continue;
            }
            if ("image".equals(mediaCategory)) {
                parts.add(AgentModelMessageContentPart.imageUrl(imageDataUrl(UUID.fromString(fileId), maxImageBytes)));
            } else if ("pdf".equals(mediaCategory)
                    && "RENDER_TO_IMAGES_WHEN_NO_TEXT".equals(pdfHandlingMode)
                    && !hasTextContent(source)) {
                for (var dataUrl : pdfPageDataUrls(UUID.fromString(fileId), maxImageBytes, maxPdfRenderedPages)) {
                    parts.add(AgentModelMessageContentPart.imageUrl(dataUrl));
                }
            }
        }
        return List.copyOf(parts);
    }

    private String imageDataUrl(UUID fileId, int maxImageBytes) {
        var content = storedFiles
                .openContent(fileId)
                .orElseThrow(() -> new IllegalStateException("Lesson source image was not found: " + fileId));
        try (content) {
            if (content.sizeBytes() > maxImageBytes) {
                throw new IllegalStateException("Lesson source image must be at most 5 MB for multimodal input");
            }
            var bytes = content.inputStream().readNBytes(maxImageBytes + 1);
            if (bytes.length > maxImageBytes) {
                throw new IllegalStateException("Lesson source image must be at most 5 MB for multimodal input");
            }
            var contentType = content.contentType().orElse("image/jpeg");
            return "data:%s;base64,%s".formatted(contentType, Base64.getEncoder().encodeToString(bytes));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read lesson source image for multimodal input", ex);
        }
    }

    private List<String> pdfPageDataUrls(UUID fileId, int maxImageBytes, int maxPdfRenderedPages) {
        var content = storedFiles
                .openContent(fileId)
                .orElseThrow(() -> new IllegalStateException("Lesson source PDF was not found: " + fileId));
        try (content) {
            if (content.sizeBytes() > MAX_IMAGE_BYTES) {
                throw new IllegalStateException("Lesson source PDF must be at most 5 MB for multimodal input");
            }
            var bytes = content.inputStream().readNBytes(MAX_IMAGE_BYTES + 1);
            if (bytes.length > MAX_IMAGE_BYTES) {
                throw new IllegalStateException("Lesson source PDF must be at most 5 MB for multimodal input");
            }
            return pdfPageRenderer.renderPngDataUrls(bytes, maxPdfRenderedPages, maxImageBytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read lesson source PDF for multimodal input", ex);
        }
    }

    private static boolean hasTextContent(JsonNode source) {
        var text = source.path("textContent").asText(null);
        return text != null && !text.isBlank();
    }

    private static int positiveInt(JsonNode node, int defaultValue) {
        if (!node.canConvertToInt()) {
            return defaultValue;
        }
        var value = node.asInt();
        return value > 0 ? value : defaultValue;
    }

    private String describeOutputContract(Map<String, ?> requiredFields) {
        var joiner = new StringJoiner("\n");
        requiredFields.forEach((fieldName, fieldType) -> joiner.add("- " + fieldName + ": " + fieldType));
        return joiner.toString();
    }

    private String prettyPrintJson(String rawJson) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(rawJson));
        } catch (JsonProcessingException ex) {
            return rawJson;
        }
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Lesson source multimodal input must be valid JSON", ex);
        }
    }

    private String renderSections(
            List<ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection> sections) {
        var joiner = new StringJoiner("\n\n");
        for (var section : sections) {
            joiner.add("### %s\n%s".formatted(section.title(), section.content()));
        }
        return joiner.toString();
    }
}
