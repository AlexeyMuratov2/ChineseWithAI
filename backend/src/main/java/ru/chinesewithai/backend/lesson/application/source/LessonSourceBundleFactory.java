package ru.chinesewithai.backend.lesson.application.source;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

@Component
public class LessonSourceBundleFactory {

    private static final int MAX_PDF_TEXT_EXTRACTION_BYTES = 5 * 1024 * 1024;

    private final StoredFileFacade storedFiles;
    private final LessonPdfTextExtractor pdfTextExtractor;

    public LessonSourceBundleFactory(StoredFileFacade storedFiles, LessonPdfTextExtractor pdfTextExtractor) {
        this.storedFiles = storedFiles;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    public LessonSourceBundle build(LessonDraftView draft, LessonSourceProcessingPolicy policy) {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        validateSourceCount(draft, policy);

        var items = draft.sources().stream()
                .sorted(Comparator.comparingInt(LessonDraftSourceView::position))
                .map(source -> toManifestItem(source, policy))
                .toList();
        return new LessonSourceBundle(1, items);
    }

    private LessonSourceManifestItem toManifestItem(
            LessonDraftSourceView source, LessonSourceProcessingPolicy policy) {
        if (!policy.allowedSourceTypes().contains(source.type())) {
            throw new LessonContentValidationException("Source type is not allowed for lesson module: " + source.type());
        }
        if ("TEXT_NOTE".equals(source.type())) {
            var text = normalizeText(source.textContent());
            if (text == null) {
                throw new LessonContentValidationException("TEXT_NOTE source must have non-empty textContent");
            }
            validateMediaCategory("text", policy);
            return new LessonSourceManifestItem(
                    source.id(),
                    source.type(),
                    source.position(),
                    text,
                    null,
                    null,
                    "text/plain",
                    (long) text.length(),
                    "text");
        }

        if (!"DOCUMENT_FILE".equals(source.type())) {
            throw new LessonContentValidationException("Unsupported lesson source type: " + source.type());
        }
        if (source.documentFileId() == null) {
            throw new LessonContentValidationException("DOCUMENT_FILE source must have documentFileId");
        }

        var metadata = storedFiles
                .getMetadata(source.documentFileId())
                .orElseThrow(() -> new LessonContentValidationException(
                        "DOCUMENT_FILE source metadata was not found: " + source.documentFileId()));
        var contentType = normalizeContentType(metadata.contentType().orElse(null));
        var originalFileName = firstNonBlank(source.documentOriginalFileName(), metadata.originalFileName().orElse(null));
        var mediaCategory = mediaCategory(contentType, originalFileName);
        validateMediaCategory(mediaCategory, policy);
        var textContent = documentTextContent(source, mediaCategory, policy);

        return new LessonSourceManifestItem(
                source.id(),
                source.type(),
                source.position(),
                textContent,
                source.documentFileId(),
                originalFileName,
                contentType,
                metadata.sizeBytes(),
                mediaCategory);
    }

    private String documentTextContent(
            LessonDraftSourceView source, String mediaCategory, LessonSourceProcessingPolicy policy) {
        var existingText = normalizeText(source.textContent());
        if (existingText != null || !"pdf".equals(mediaCategory)) {
            return existingText;
        }
        if (policy.pdfHandlingMode() != PdfHandlingMode.EMBEDDED_TEXT_FIRST
                && policy.pdfHandlingMode() != PdfHandlingMode.RENDER_TO_IMAGES_WHEN_NO_TEXT) {
            return null;
        }
        var content = storedFiles.openContent(source.documentFileId());
        if (content.isEmpty()) {
            return null;
        }
        try (var pdfContent = content.get()) {
            if (pdfContent.sizeBytes() > MAX_PDF_TEXT_EXTRACTION_BYTES) {
                return null;
            }
            var bytes = pdfContent.inputStream().readNBytes(MAX_PDF_TEXT_EXTRACTION_BYTES + 1);
            if (bytes.length > MAX_PDF_TEXT_EXTRACTION_BYTES) {
                return null;
            }
            return pdfTextExtractor.extractText(bytes).orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    private void validateSourceCount(LessonDraftView draft, LessonSourceProcessingPolicy policy) {
        var count = draft.sources().size();
        if (count < policy.minSources() || count > policy.maxSources()) {
            throw new LessonContentValidationException(
                    "Lesson source count must be between %d and %d".formatted(policy.minSources(), policy.maxSources()));
        }
    }

    private void validateMediaCategory(String mediaCategory, LessonSourceProcessingPolicy policy) {
        if (!policy.allowedMediaCategories().contains(mediaCategory)) {
            throw new LessonContentValidationException(
                    "Source media category is not allowed for lesson module: " + mediaCategory);
        }
    }

    private static String mediaCategory(String contentType, String originalFileName) {
        if ((contentType != null && contentType.startsWith("image/"))
                || hasExtension(originalFileName, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif")) {
            return "image";
        }
        if ("application/pdf".equals(contentType) || hasExtension(originalFileName, ".pdf")) {
            return "pdf";
        }
        if ((contentType != null && contentType.startsWith("text/"))
                || hasExtension(originalFileName, ".txt", ".md", ".csv", ".json")) {
            return "text";
        }
        return "binary";
    }

    private static boolean hasExtension(String fileName, String... extensions) {
        if (fileName == null) {
            return false;
        }
        var normalized = fileName.trim().toLowerCase(Locale.ROOT);
        for (var extension : extensions) {
            if (normalized.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeContentType(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        var separatorIndex = normalized.indexOf(';');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(0, separatorIndex).trim();
        }
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String firstNonBlank(String first, String second) {
        var normalizedFirst = normalizeText(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return normalizeText(second);
    }
}
