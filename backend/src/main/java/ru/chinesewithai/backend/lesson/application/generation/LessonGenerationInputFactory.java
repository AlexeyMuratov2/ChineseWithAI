package ru.chinesewithai.backend.lesson.application.generation;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

@Component
public class LessonGenerationInputFactory {

    private static final int MAX_MODEL_SOURCE_BYTES = 5 * 1024 * 1024;

    private final StoredFileFacade storedFiles;

    public LessonGenerationInputFactory(StoredFileFacade storedFiles) {
        this.storedFiles = storedFiles;
    }

    public LinkedHashMap<String, Object> build(LessonDraftView draft, LessonModule module) {
        var orderedSources = draft.sources().stream()
                .map(this::toSourcePayload)
                .toList();

        var draftPayload = new LinkedHashMap<String, Object>();
        draftPayload.put("id", draft.id());
        draftPayload.put("title", draft.title());
        draftPayload.put("description", draft.description());
        draftPayload.put("userInstructions", draft.userInstructions());
        draftPayload.put("explanationLanguage", draft.explanationLanguage());
        draftPayload.put("translationLanguage", draft.translationLanguage());
        draftPayload.put("sources", orderedSources);

        var input = new LinkedHashMap<String, Object>();
        input.put("draftId", draft.id());
        input.put("moduleKey", module.moduleKey());
        input.put("moduleSchemaVersion", module.schemaVersion());
        input.put("draft", draftPayload);
        input.put("orderedSources", List.copyOf(orderedSources));
        if (!orderedSources.isEmpty()) {
            input.put("primarySource", orderedSources.getFirst());
        }
        return input;
    }

    private LinkedHashMap<String, Object> toSourcePayload(LessonDraftSourceView source) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("id", source.id());
        payload.put("type", source.type());
        payload.put("position", source.position());
        payload.put("textContent", source.textContent());
        payload.put("documentFileId", source.documentFileId());
        payload.put("documentOriginalFileName", source.documentOriginalFileName());
        if (source.documentFileId() != null) {
            payload.put("fileContent", buildFileContentPayload(source));
        }
        return payload;
    }

    private LinkedHashMap<String, Object> buildFileContentPayload(LessonDraftSourceView source) {
        var content = storedFiles
                .openContent(source.documentFileId())
                .orElseThrow(() -> new IllegalStateException(
                        "Lesson draft source file was not found: " + source.documentFileId()));

        try (content) {
            if (content.sizeBytes() > MAX_MODEL_SOURCE_BYTES) {
                throw new IllegalStateException("Lesson draft source file must be at most 5 MB for model input");
            }
            var bytes = content.inputStream().readNBytes(MAX_MODEL_SOURCE_BYTES + 1);
            if (bytes.length > MAX_MODEL_SOURCE_BYTES) {
                throw new IllegalStateException("Lesson draft source file must be at most 5 MB for model input");
            }

            var contentType = normalizeContentType(content.contentType().orElse(null));
            var originalFileName = source.documentOriginalFileName() != null
                    ? source.documentOriginalFileName()
                    : content.originalFileName().orElse(null);

            var payload = new LinkedHashMap<String, Object>();
            payload.put("fileId", source.documentFileId());
            payload.put("originalFileName", originalFileName);
            payload.put("contentType", contentType);
            payload.put("mediaCategory", mediaCategory(contentType, originalFileName));
            payload.put("sizeBytes", content.sizeBytes());
            payload.put("contentEncoding", "base64");
            payload.put("contentBase64", Base64.getEncoder().encodeToString(bytes));
            return payload;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read lesson draft source file for model input", ex);
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
}
