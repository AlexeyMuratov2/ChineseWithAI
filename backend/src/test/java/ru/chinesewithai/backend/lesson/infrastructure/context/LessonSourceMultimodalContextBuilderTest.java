package ru.chinesewithai.backend.lesson.infrastructure.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentContextBuildRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageContentPartType;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationState;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileContent;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

class LessonSourceMultimodalContextBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StoredFileFacade storedFiles = mock(StoredFileFacade.class);
    private final LessonSourceMultimodalContextBuilder builder =
            new LessonSourceMultimodalContextBuilder(objectMapper, storedFiles, new LessonPdfPageRenderer());

    @Test
    void createsTextAndImageContentPartsFromSourceBundleManifest() throws Exception {
        var fileId = UUID.randomUUID();
        var bytes = "fake-png".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(new StoredFileContent(
                        new ByteArrayInputStream(bytes),
                        bytes.length,
                        Optional.of("image/png"),
                        Optional.of("page.png"),
                        () -> {})));

        var inputJson = objectMapper.writeValueAsString(Map.of(
                "sourceBundle", Map.of(
                        "sourceBundleVersion", 1,
                        "sources", List.of(Map.of(
                                "sourceId", UUID.randomUUID().toString(),
                                "type", "DOCUMENT_FILE",
                                "position", 0,
                                "fileId", fileId.toString(),
                                "originalFileName", "page.png",
                                "contentType", "image/png",
                                "sizeBytes", bytes.length,
                                "mediaCategory", "image")))));

        var messages = builder.buildContext(new AgentContextBuildRequest(
                profile(),
                AgentSession.createNew("lesson-stage:hsk5_v2_source_normalizer", "fake-model", "Normalize", inputJson, Instant.now()),
                List.of(),
                PreGenerationState.empty()));

        var userMessage = messages.get(1);
        assertThat(userMessage.content()).isNull();
        assertThat(userMessage.contentParts()).hasSize(2);
        assertThat(userMessage.contentParts().get(0).type()).isEqualTo(AgentModelMessageContentPartType.TEXT);
        assertThat(userMessage.contentParts().get(1).type()).isEqualTo(AgentModelMessageContentPartType.IMAGE_URL);
        assertThat(userMessage.contentParts().get(1).imageUrl()).startsWith("data:image/png;base64,");
    }

    @Test
    void rendersPdfWithoutTextAsImageContentParts() throws Exception {
        var fileId = UUID.randomUUID();
        var bytes = blankPdf();
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(new StoredFileContent(
                        new ByteArrayInputStream(bytes),
                        bytes.length,
                        Optional.of("application/pdf"),
                        Optional.of("page.pdf"),
                        () -> {})));

        var inputJson = objectMapper.writeValueAsString(Map.of(
                "sourceProcessingPolicy", Map.of(
                        "pdfHandlingMode", "RENDER_TO_IMAGES_WHEN_NO_TEXT",
                        "attachImagesToVisionStages", true,
                        "maxInlineImageBytes", 5 * 1024 * 1024,
                        "maxPdfRenderedPages", 3),
                "sourceBundle", Map.of(
                        "sourceBundleVersion", 1,
                        "sources", List.of(Map.of(
                                "sourceId", UUID.randomUUID().toString(),
                                "type", "DOCUMENT_FILE",
                                "position", 0,
                                "fileId", fileId.toString(),
                                "originalFileName", "page.pdf",
                                "contentType", "application/pdf",
                                "sizeBytes", bytes.length,
                                "mediaCategory", "pdf")))));

        var messages = builder.buildContext(new AgentContextBuildRequest(
                profile(),
                AgentSession.createNew(
                        "lesson-stage:hsk5_v2_source_normalizer", "fake-model", "Normalize", inputJson, Instant.now()),
                List.of(),
                PreGenerationState.empty()));

        var userMessage = messages.get(1);
        assertThat(userMessage.contentParts()).hasSize(2);
        assertThat(userMessage.contentParts().get(1).type()).isEqualTo(AgentModelMessageContentPartType.IMAGE_URL);
        assertThat(userMessage.contentParts().get(1).imageUrl()).startsWith("data:image/png;base64,");
    }

    private AgentProfile profile() {
        return new AgentProfile(
                "lesson-stage:hsk5_v2_source_normalizer",
                "Source normalizer",
                "Return JSON.",
                LessonSourceMultimodalContextBuilder.KEY,
                List.of(),
                new ExecutionPolicy(2),
                new MemoryPolicy(true, 4),
                OutputContract.ofRequiredFields(Map.of(
                        "sourcePackVersion", OutputFieldType.NUMBER,
                        "sources", OutputFieldType.ARRAY)),
                true,
                false);
    }

    private byte[] blankPdf() throws Exception {
        try (var document = new PDDocument();
                var out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(out);
            return out.toByteArray();
        }
    }
}
