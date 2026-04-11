package ru.chinesewithai.backend.lessondraft.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class LessonDraftSource {

    private static final int MAX_TEXT_NOTE_LENGTH = 20_000;
    private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;

    private final LessonDraftSourceId id;
    private final LessonDraftSourceType type;
    private final int position;
    private final String textContent;
    private final UUID documentFileId;
    private final String documentOriginalFileName;
    private final Instant createdAt;
    private final Instant updatedAt;

    private LessonDraftSource(
            LessonDraftSourceId id,
            LessonDraftSourceType type,
            int position,
            String textContent,
            UUID documentFileId,
            String documentOriginalFileName,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.position = validatePosition(position);
        this.textContent = textContent;
        this.documentFileId = documentFileId;
        this.documentOriginalFileName = documentOriginalFileName;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        validatePayloadByType(type, textContent, documentFileId, documentOriginalFileName);
    }

    public static LessonDraftSource createTextNote(int position, String textContent, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new LessonDraftSource(
                LessonDraftSourceId.newId(),
                LessonDraftSourceType.TEXT_NOTE,
                position,
                normalizeTextNote(textContent),
                null,
                null,
                now,
                now);
    }

    public static LessonDraftSource createDocumentFile(int position, UUID documentFileId, String originalFileName, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new LessonDraftSource(
                LessonDraftSourceId.newId(),
                LessonDraftSourceType.DOCUMENT_FILE,
                position,
                null,
                Objects.requireNonNull(documentFileId, "documentFileId must not be null"),
                normalizeOriginalFileName(originalFileName),
                now,
                now);
    }

    public static LessonDraftSource reconstitute(
            LessonDraftSourceId id,
            LessonDraftSourceType type,
            int position,
            String textContent,
            UUID documentFileId,
            String documentOriginalFileName,
            Instant createdAt,
            Instant updatedAt) {
        return new LessonDraftSource(
                id,
                type,
                position,
                normalizeTextNoteNullable(textContent),
                documentFileId,
                normalizeOriginalFileName(documentOriginalFileName),
                createdAt,
                updatedAt);
    }

    public LessonDraftSource reposition(int newPosition, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new LessonDraftSource(
                id, type, newPosition, textContent, documentFileId, documentOriginalFileName, createdAt, now);
    }

    public LessonDraftSourceId id() {
        return id;
    }

    public LessonDraftSourceType type() {
        return type;
    }

    public int position() {
        return position;
    }

    public String textContent() {
        return textContent;
    }

    public UUID documentFileId() {
        return documentFileId;
    }

    public String documentOriginalFileName() {
        return documentOriginalFileName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static void validatePayloadByType(
            LessonDraftSourceType type, String textContent, UUID documentFileId, String documentOriginalFileName) {
        switch (type) {
            case TEXT_NOTE -> {
                if (textContent == null || textContent.isBlank()) {
                    throw new IllegalArgumentException("textContent must be present for TEXT_NOTE");
                }
                if (documentFileId != null || documentOriginalFileName != null) {
                    throw new IllegalArgumentException("document payload must be null for TEXT_NOTE");
                }
            }
            case DOCUMENT_FILE -> {
                if (documentFileId == null) {
                    throw new IllegalArgumentException("documentFileId must be present for DOCUMENT_FILE");
                }
                if (textContent != null) {
                    throw new IllegalArgumentException("textContent must be null for DOCUMENT_FILE");
                }
            }
            default -> throw new IllegalArgumentException("unsupported source type: " + type);
        }
    }

    private static int validatePosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("position must be >= 0");
        }
        return position;
    }

    private static String normalizeTextNote(String value) {
        var normalized = normalizeTextNoteNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("textContent must not be blank");
        }
        return normalized;
    }

    private static String normalizeTextNoteNullable(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_TEXT_NOTE_LENGTH) {
            throw new IllegalArgumentException("textContent must be at most " + MAX_TEXT_NOTE_LENGTH + " chars");
        }
        return normalized;
    }

    private static String normalizeOriginalFileName(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_ORIGINAL_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "documentOriginalFileName must be at most " + MAX_ORIGINAL_FILE_NAME_LENGTH + " chars");
        }
        return normalized;
    }
}
