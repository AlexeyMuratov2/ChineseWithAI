package ru.chinesewithai.backend.lesson.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class LessonVocabularyItem {

    private final Long id;
    private final LessonId lessonId;
    private final UUID userId;
    private final String hanzi;
    private final String pinyin;
    private final String translation;
    private final LanguageTag translationLanguage;
    private final Instant createdAt;

    private LessonVocabularyItem(
            Long id,
            LessonId lessonId,
            UUID userId,
            String hanzi,
            String pinyin,
            String translation,
            LanguageTag translationLanguage,
            Instant createdAt) {
        if (id != null && id < 0L) {
            throw new IllegalArgumentException("id must be >= 0");
        }
        this.id = id;
        this.lessonId = Objects.requireNonNull(lessonId, "lessonId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.hanzi = requireText(hanzi, "hanzi");
        this.pinyin = requireText(pinyin, "pinyin");
        this.translation = requireText(translation, "translation");
        this.translationLanguage = Objects.requireNonNull(translationLanguage, "translationLanguage must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static LessonVocabularyItem createNew(
            LessonId lessonId,
            UUID userId,
            String hanzi,
            String pinyin,
            String translation,
            LanguageTag translationLanguage,
            Instant createdAt) {
        return new LessonVocabularyItem(null, lessonId, userId, hanzi, pinyin, translation, translationLanguage, createdAt);
    }

    public static LessonVocabularyItem reconstitute(
            Long id,
            LessonId lessonId,
            UUID userId,
            String hanzi,
            String pinyin,
            String translation,
            LanguageTag translationLanguage,
            Instant createdAt) {
        return new LessonVocabularyItem(id, lessonId, userId, hanzi, pinyin, translation, translationLanguage, createdAt);
    }

    public Long id() {
        return id;
    }

    public LessonId lessonId() {
        return lessonId;
    }

    public UUID userId() {
        return userId;
    }

    public String hanzi() {
        return hanzi;
    }

    public String pinyin() {
        return pinyin;
    }

    public String translation() {
        return translation;
    }

    public LanguageTag translationLanguage() {
        return translationLanguage;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        var normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
