package ru.chinesewithai.backend.lessondraft.application.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lessondraft.application.exception.InvalidSourcePayloadException;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileContent;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

@Component
public class LessonDraftDocumentTextExtractor {

    private static final int MAX_TEXT_SOURCE_CHARS = 20_000;
    private static final int MAX_TEXT_SOURCE_BYTES = 200_000;
    private static final long MAX_BINARY_SOURCE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> TEXT_FILE_EXTENSIONS = Set.of(".txt", ".md", ".csv", ".json");
    private static final Set<String> IMAGE_FILE_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif");
    private static final Set<String> TEXT_CONTENT_TYPES =
            Set.of("application/json", "application/x-ndjson", "application/xml", "text/markdown");

    private final StoredFileFacade storedFiles;

    public LessonDraftDocumentTextExtractor(StoredFileFacade storedFiles) {
        this.storedFiles = storedFiles;
    }

    public ExtractedLessonDraftDocumentSource extract(UUID documentFileId, String requestedOriginalFileName) {
        var content = storedFiles
                .openContent(documentFileId)
                .orElseThrow(() -> new InvalidSourcePayloadException("documentFileId was not found"));

        try (content) {
            var originalFileName = resolveOriginalFileName(requestedOriginalFileName, content);
            var contentType = normalizeContentType(content.contentType().orElse(null));

            if (isTextSource(contentType, originalFileName)) {
                return extractText(content, originalFileName);
            }

            if (isPdfSource(contentType, originalFileName) || isImageSource(contentType, originalFileName)) {
                validateBinarySourceSize(content.sizeBytes());
                return new ExtractedLessonDraftDocumentSource(null, originalFileName);
            }

            throw new InvalidSourcePayloadException("document source must be a UTF-8 text file, PDF, or image");
        } catch (IOException ex) {
            throw new InvalidSourcePayloadException("Failed to read document source");
        }
    }

    private static ExtractedLessonDraftDocumentSource extractText(StoredFileContent content, String originalFileName)
            throws IOException {
        if (content.sizeBytes() > MAX_TEXT_SOURCE_BYTES) {
            throw new InvalidSourcePayloadException("document text source must be at most 200 KB");
        }

        var bytes = content.inputStream().readNBytes(MAX_TEXT_SOURCE_BYTES + 1);
        if (bytes.length > MAX_TEXT_SOURCE_BYTES) {
            throw new InvalidSourcePayloadException("document text source must be at most 200 KB");
        }

        var text = stripUtf8Bom(decodeUtf8(bytes)).trim();
        if (text.isBlank()) {
            throw new InvalidSourcePayloadException("document source text must not be blank");
        }
        if (text.indexOf('\u0000') >= 0) {
            throw new InvalidSourcePayloadException("document source must be a UTF-8 text file");
        }
        if (text.length() > MAX_TEXT_SOURCE_CHARS) {
            throw new InvalidSourcePayloadException("document source text must be at most 20000 chars");
        }

        return new ExtractedLessonDraftDocumentSource(text, originalFileName);
    }

    private static void validateBinarySourceSize(long sizeBytes) {
        if (sizeBytes > MAX_BINARY_SOURCE_BYTES) {
            throw new InvalidSourcePayloadException("PDF/image lesson source must be at most 5 MB");
        }
    }

    private static String decodeUtf8(byte[] bytes) {
        var decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            throw new InvalidSourcePayloadException("document source must be a UTF-8 text file");
        }
    }

    private static String stripUtf8Bom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private static String resolveOriginalFileName(String requestedOriginalFileName, StoredFileContent content) {
        var requested = normalizeOptional(requestedOriginalFileName);
        if (requested != null) {
            return requested;
        }
        return content.originalFileName().map(LessonDraftDocumentTextExtractor::normalizeOptional).orElse(null);
    }

    private static boolean isTextSource(String contentType, String originalFileName) {
        return (contentType != null && (contentType.startsWith("text/") || TEXT_CONTENT_TYPES.contains(contentType)))
                || hasExtension(originalFileName, TEXT_FILE_EXTENSIONS);
    }

    private static boolean isPdfSource(String contentType, String originalFileName) {
        return "application/pdf".equals(contentType) || hasExtension(originalFileName, Set.of(".pdf"));
    }

    private static boolean isImageSource(String contentType, String originalFileName) {
        return (contentType != null && contentType.startsWith("image/"))
                || hasExtension(originalFileName, IMAGE_FILE_EXTENSIONS);
    }

    private static boolean hasExtension(String fileName, Set<String> extensions) {
        if (fileName == null) {
            return false;
        }
        var normalized = fileName.trim().toLowerCase(Locale.ROOT);
        return extensions.stream().anyMatch(normalized::endsWith);
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

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
