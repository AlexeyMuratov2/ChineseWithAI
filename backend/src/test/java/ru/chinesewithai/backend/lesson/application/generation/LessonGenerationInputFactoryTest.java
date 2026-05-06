package ru.chinesewithai.backend.lesson.application.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileContent;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

class LessonGenerationInputFactoryTest {

    private final StoredFileFacade storedFiles = mock(StoredFileFacade.class);
    private final LessonGenerationInputFactory factory = new LessonGenerationInputFactory(storedFiles);

    @Test
    void includesUploadedFileContentForModelInput() {
        var fileId = UUID.randomUUID();
        var bytes = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(content("application/pdf", "source.pdf", bytes)));

        var input = factory.build(draft(fileId), module());

        var source = firstSource(input);
        var fileContent = asMap(source.get("fileContent"));
        assertThat(fileContent.get("contentType")).isEqualTo("application/pdf");
        assertThat(fileContent.get("mediaCategory")).isEqualTo("pdf");
        assertThat(fileContent.get("contentEncoding")).isEqualTo("base64");
        assertThat(fileContent.get("contentBase64")).isEqualTo(Base64.getEncoder().encodeToString(bytes));
    }

    private static LessonDraftView draft(UUID fileId) {
        return new LessonDraftView(
                UUID.randomUUID(),
                "Draft",
                null,
                null,
                "ru",
                "ru",
                List.of(new LessonDraftSourceView(
                        UUID.randomUUID(), "DOCUMENT_FILE", 0, null, fileId, "source.pdf", Instant.now(), Instant.now())),
                Instant.now(),
                Instant.now(),
                0L);
    }

    private static LessonModule module() {
        return new LessonModule(
                "hsk5_v1",
                "HSK 5 v1",
                "Module prompt",
                1,
                true,
                "lesson-generator:hsk5_v1_composer",
                null,
                Hsk5QualityLessonGenerationPipeline.PIPELINE_KEY,
                Instant.now(),
                Instant.now());
    }

    private static StoredFileContent content(String contentType, String originalFileName, byte[] bytes) {
        return new StoredFileContent(
                new ByteArrayInputStream(bytes),
                bytes.length,
                Optional.ofNullable(contentType),
                Optional.ofNullable(originalFileName),
                () -> {});
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstSource(Map<String, Object> input) {
        return (Map<String, Object>) ((List<?>) input.get("orderedSources")).getFirst();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }
}
