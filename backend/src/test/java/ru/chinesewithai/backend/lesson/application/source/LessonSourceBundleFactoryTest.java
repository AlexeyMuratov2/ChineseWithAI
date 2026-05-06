package ru.chinesewithai.backend.lesson.application.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileContent;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileMetadata;

class LessonSourceBundleFactoryTest {

    private final StoredFileFacade storedFiles = mock(StoredFileFacade.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LessonSourceBundleFactory factory = new LessonSourceBundleFactory(storedFiles, new LessonPdfTextExtractor());

    @Test
    void buildsOrderedManifestWithoutRawFileBytes() throws Exception {
        var fileId = UUID.randomUUID();
        when(storedFiles.getMetadata(fileId))
                .thenReturn(Optional.of(new StoredFileMetadata(
                        fileId,
                        1234,
                        Optional.of("image/png"),
                        Optional.of("page.png"),
                        Instant.now())));

        var bundle = factory.build(
                draft(List.of(textSource("source text", 0), documentSource(fileId, "page.png", 1))),
                LessonSourceProcessingPolicies.hsk5V2NormalizeFirst());

        assertThat(bundle.sources()).hasSize(2);
        assertThat(bundle.sources()).extracting(LessonSourceManifestItem::mediaCategory)
                .containsExactly("text", "image");
        var rawJson = objectMapper.writeValueAsString(bundle);
        assertThat(rawJson).contains(fileId.toString());
        assertThat(rawJson).doesNotContain("contentBase64");
        assertThat(rawJson).doesNotContain("contentEncoding");
    }

    @Test
    void extractsEmbeddedPdfTextIntoManifest() throws Exception {
        var fileId = UUID.randomUUID();
        var bytes = pdfWithText("embedded source text");
        when(storedFiles.getMetadata(fileId))
                .thenReturn(Optional.of(new StoredFileMetadata(
                        fileId,
                        bytes.length,
                        Optional.of("application/pdf"),
                        Optional.of("page.pdf"),
                        Instant.now())));
        when(storedFiles.openContent(fileId))
                .thenReturn(Optional.of(new StoredFileContent(
                        new ByteArrayInputStream(bytes),
                        bytes.length,
                        Optional.of("application/pdf"),
                        Optional.of("page.pdf"),
                        () -> {})));

        var bundle = factory.build(
                draft(List.of(documentSource(fileId, "page.pdf", 0))),
                LessonSourceProcessingPolicies.hsk5V2NormalizeFirst());

        assertThat(bundle.sources()).hasSize(1);
        assertThat(bundle.sources().getFirst().mediaCategory()).isEqualTo("pdf");
        assertThat(bundle.sources().getFirst().textContent()).contains("embedded source text");
    }

    private LessonDraftView draft(List<LessonDraftSourceView> sources) {
        return new LessonDraftView(
                UUID.randomUUID(),
                "Draft",
                null,
                null,
                "ru",
                "ru",
                sources,
                Instant.now(),
                Instant.now(),
                0L);
    }

    private LessonDraftSourceView textSource(String text, int position) {
        return new LessonDraftSourceView(
                UUID.randomUUID(), "TEXT_NOTE", position, text, null, null, Instant.now(), Instant.now());
    }

    private LessonDraftSourceView documentSource(UUID fileId, String fileName, int position) {
        return new LessonDraftSourceView(
                UUID.randomUUID(), "DOCUMENT_FILE", position, null, fileId, fileName, Instant.now(), Instant.now());
    }

    private byte[] pdfWithText(String text) throws Exception {
        try (var document = new PDDocument();
                var out = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
