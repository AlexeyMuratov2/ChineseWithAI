package ru.chinesewithai.backend.lessondraft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lessondraft.application.exception.InvalidSourcePayloadException;
import ru.chinesewithai.backend.lessondraft.application.service.LessonDraftDocumentTextExtractor;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileContent;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

class LessonDraftDocumentTextExtractorTest {

    private final StoredFileFacade storedFiles = mock(StoredFileFacade.class);
    private final LessonDraftDocumentTextExtractor extractor = new LessonDraftDocumentTextExtractor(storedFiles);

    @Test
    void extractsUtf8TextFileContent() {
        var fileId = UUID.randomUUID();
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(content("text/plain", "source.txt", "机场见面以后".getBytes(StandardCharsets.UTF_8))));

        var extracted = extractor.extract(fileId, null);

        assertThat(extracted.textContent()).isEqualTo("机场见面以后");
        assertThat(extracted.originalFileName()).isEqualTo("source.txt");
    }

    @Test
    void acceptsPdfWithoutExtractingText() {
        var fileId = UUID.randomUUID();
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(content("application/pdf", "lesson.pdf", "%PDF-1.7".getBytes(StandardCharsets.UTF_8))));

        var extracted = extractor.extract(fileId, null);

        assertThat(extracted.textContent()).isNull();
        assertThat(extracted.originalFileName()).isEqualTo("lesson.pdf");
    }

    @Test
    void acceptsImageWithoutExtractingText() {
        var fileId = UUID.randomUUID();
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(content("image/jpeg", "photo.jpg", new byte[] {(byte) 0xff, (byte) 0xd8, 0x00})));

        var extracted = extractor.extract(fileId, null);

        assertThat(extracted.textContent()).isNull();
        assertThat(extracted.originalFileName()).isEqualTo("photo.jpg");
    }

    @Test
    void rejectsUnsupportedBinaryFile() {
        var fileId = UUID.randomUUID();
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(content("application/octet-stream", "archive.bin", new byte[] {0, 1, 2})));

        assertThatThrownBy(() -> extractor.extract(fileId, null))
                .isInstanceOf(InvalidSourcePayloadException.class)
                .hasMessageContaining("UTF-8 text file, PDF, or image");
    }

    private static StoredFileContent content(String contentType, String originalFileName, byte[] bytes) {
        return new StoredFileContent(
                new ByteArrayInputStream(bytes),
                bytes.length,
                Optional.ofNullable(contentType),
                Optional.ofNullable(originalFileName),
                () -> {});
    }
}
