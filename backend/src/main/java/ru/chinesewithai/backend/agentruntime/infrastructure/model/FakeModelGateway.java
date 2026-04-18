package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageRole;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelResponse;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolCall;

@Component
public class FakeModelGateway implements AgentModelGateway {

    static final String MODEL_KEY = "fake-model";
    private static final String PROVIDER_KEY = "fake";
    private static final String STATIC_TOOL_NAME = "get_static_test_data";
    private static final String LESSON_GENERATOR_PROFILE_KEY = "lesson-generator:v1";
    private static final String INVALID_OUTPUT_MARKER = "[[INVALID_LESSON_OUTPUT]]";

    private final ObjectMapper objectMapper;

    public FakeModelGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerKey() {
        return PROVIDER_KEY;
    }

    @Override
    public List<AgentModelDescriptor> supportedModels() {
        return List.of(new AgentModelDescriptor(MODEL_KEY, "Fake Model", PROVIDER_KEY, false));
    }

    @Override
    public AgentModelResponse generate(AgentModelRequest request) {
        if (LESSON_GENERATOR_PROFILE_KEY.equals(request.profile().profileKey())) {
            return generateLesson(request);
        }

        var toolMessage = request.messages().stream()
                .filter(message ->
                        message.role() == AgentModelMessageRole.TOOL && STATIC_TOOL_NAME.equals(message.name()))
                .findFirst()
                .map(message -> readJson(message.content()).path("toolMessage").asText(null))
                .orElse(null);

        if (toolMessage == null) {
            var toolCall = new AgentToolCall("fake-tool-call-1", STATIC_TOOL_NAME, "{}");
            var rawPayload = writeJson(Map.of(
                    "type", "TOOL_CALL",
                    "toolCalls", List.of(Map.of(
                            "id", toolCall.toolCallId(),
                            "toolName", toolCall.toolName(),
                            "arguments", Map.of()))));
            return AgentModelResponse.toolCalls(rawPayload, List.of(toolCall));
        }

        var finalOutput = writeJson(Map.of(
                "summary", "Fake agent completed successfully",
                "toolMessage", toolMessage));
        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private AgentModelResponse generateLesson(AgentModelRequest request) {
        var input = readJson(request.session().inputJson());
        var draft = input.path("draft");
        var explanationLanguage = draft.path("explanationLanguage").asText("zh");
        var translationLanguage = draft.path("translationLanguage").asText("en");
        var title = draft.path("title").asText("Test Lesson");
        var sourceText = draft.path("sources").isArray() && !draft.path("sources").isEmpty()
                ? draft.path("sources").get(0).path("textContent").asText("")
                : "";

        final String finalOutput;
        if (sourceText.contains(INVALID_OUTPUT_MARKER)) {
            finalOutput = writeJson(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "TestModule",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "newWords", List.of(),
                    "sections", List.of(Map.of("type", "reading", "title", "Broken"))));
        } else {
            var wordUsageTitle = explanationLanguage.startsWith("zh") ? "先看新词" : "New Words";
            var readingTitle = explanationLanguage.startsWith("zh") ? "短文" : "Reading";
            finalOutput = writeJson(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "TestModule",
                    "title", title,
                    "studyLanguage", "zh",
                    "explanationLanguage", explanationLanguage,
                    "translationLanguage", translationLanguage,
                    "newWords", List.of(
                            Map.of("word", "认识", "pinyin", "rènshi", "translation", "to know"),
                            Map.of("word", "学习", "pinyin", "xuéxí", "translation", "to study")),
                    "sections", List.of(
                            Map.of(
                                    "type", "word_usage",
                                    "title", wordUsageTitle,
                                    "items", List.of(
                                            Map.of(
                                                    "word", "认识",
                                                    "sentence", "我认识这个老师。",
                                                    "translation", "I know this teacher."),
                                            Map.of(
                                                    "word", "学习",
                                                    "sentence", "我每天学习中文。",
                                                    "translation", "I study Chinese every day."))),
                            Map.of(
                                    "type", "reading",
                                    "title", readingTitle,
                                    "text", "我认识这个老师，所以我每天跟他学习中文。",
                                    "translation", "I know this teacher, so I study Chinese with him every day."))));
        }

        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse fake model JSON", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize fake model payload", ex);
        }
    }
}
