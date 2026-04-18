package ru.chinesewithai.backend.lesson.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Lesson {

    private static final int MAX_TITLE_LENGTH = 160;

    private final LessonId id;
    private final UUID ownerId;
    private final String moduleKey;
    private final UUID sourceDraftId;
    private final UUID generatorSessionId;
    private final String title;
    private final LanguageTag studyLanguage;
    private final LanguageTag explanationLanguage;
    private final LanguageTag translationLanguage;
    private final String contentJson;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    private Lesson(
            LessonId id,
            UUID ownerId,
            String moduleKey,
            UUID sourceDraftId,
            UUID generatorSessionId,
            String title,
            LanguageTag studyLanguage,
            LanguageTag explanationLanguage,
            LanguageTag translationLanguage,
            String contentJson,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.moduleKey = normalizeOptionalText(moduleKey);
        this.sourceDraftId = sourceDraftId;
        this.generatorSessionId = generatorSessionId;
        this.title = normalizeRequiredText(title, "title", MAX_TITLE_LENGTH);
        this.studyLanguage = Objects.requireNonNull(studyLanguage, "studyLanguage must not be null");
        this.explanationLanguage =
                Objects.requireNonNull(explanationLanguage, "explanationLanguage must not be null");
        this.translationLanguage =
                Objects.requireNonNull(translationLanguage, "translationLanguage must not be null");
        this.contentJson = normalizeRequiredText(contentJson, "contentJson", Integer.MAX_VALUE);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (version < 0L) {
            throw new IllegalArgumentException("version must be >= 0");
        }
        this.version = version;
    }

    public static Lesson createNew(
            UUID ownerId,
            String moduleKey,
            UUID sourceDraftId,
            UUID generatorSessionId,
            String title,
            LanguageTag studyLanguage,
            LanguageTag explanationLanguage,
            LanguageTag translationLanguage,
            String contentJson,
            Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new Lesson(
                LessonId.newId(),
                ownerId,
                moduleKey,
                sourceDraftId,
                generatorSessionId,
                title,
                studyLanguage,
                explanationLanguage,
                translationLanguage,
                contentJson,
                now,
                now,
                0L);
    }

    public static Lesson reconstitute(
            LessonId id,
            UUID ownerId,
            String moduleKey,
            UUID sourceDraftId,
            UUID generatorSessionId,
            String title,
            LanguageTag studyLanguage,
            LanguageTag explanationLanguage,
            LanguageTag translationLanguage,
            String contentJson,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new Lesson(
                id,
                ownerId,
                moduleKey,
                sourceDraftId,
                generatorSessionId,
                title,
                studyLanguage,
                explanationLanguage,
                translationLanguage,
                contentJson,
                createdAt,
                updatedAt,
                version);
    }

    public LessonId id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String moduleKey() {
        return moduleKey;
    }

    public UUID sourceDraftId() {
        return sourceDraftId;
    }

    public UUID generatorSessionId() {
        return generatorSessionId;
    }

    public String title() {
        return title;
    }

    public LanguageTag studyLanguage() {
        return studyLanguage;
    }

    public LanguageTag explanationLanguage() {
        return explanationLanguage;
    }

    public LanguageTag translationLanguage() {
        return translationLanguage;
    }

    public String contentJson() {
        return contentJson;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    private static String normalizeRequiredText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        var normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " chars");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
