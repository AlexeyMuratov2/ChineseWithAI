package ru.chinesewithai.backend.lesson.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class LearnerVocabularyProgress {

    private final Long id;
    private final UUID userId;
    private final String hanzi;
    private final String pinyin;
    private final String translation;
    private final LanguageTag translationLanguage;
    private final LearnerVocabularyStatus status;
    private final Double masteryScore;
    private final Instant firstSeenAt;
    private final Instant lastReviewedAt;
    private final int reviewCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    private LearnerVocabularyProgress(
            Long id,
            UUID userId,
            String hanzi,
            String pinyin,
            String translation,
            LanguageTag translationLanguage,
            LearnerVocabularyStatus status,
            Double masteryScore,
            Instant firstSeenAt,
            Instant lastReviewedAt,
            int reviewCount,
            Instant createdAt,
            Instant updatedAt) {
        if (id != null && id < 0L) {
            throw new IllegalArgumentException("id must be >= 0");
        }
        if (masteryScore != null && (masteryScore < 0.0d || masteryScore > 1.0d)) {
            throw new IllegalArgumentException("masteryScore must be between 0 and 1");
        }
        if (reviewCount < 0) {
            throw new IllegalArgumentException("reviewCount must be >= 0");
        }
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.hanzi = requireText(hanzi, "hanzi");
        this.pinyin = requireText(pinyin, "pinyin");
        this.translation = requireText(translation, "translation");
        this.translationLanguage = Objects.requireNonNull(translationLanguage, "translationLanguage must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.masteryScore = masteryScore;
        this.firstSeenAt = Objects.requireNonNull(firstSeenAt, "firstSeenAt must not be null");
        this.lastReviewedAt = lastReviewedAt;
        this.reviewCount = reviewCount;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static LearnerVocabularyProgress createNew(
            UUID userId,
            String hanzi,
            String pinyin,
            String translation,
            LanguageTag translationLanguage,
            Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new LearnerVocabularyProgress(
                null,
                userId,
                hanzi,
                pinyin,
                translation,
                translationLanguage,
                LearnerVocabularyStatus.NEW,
                null,
                now,
                null,
                0,
                now,
                now);
    }

    public static LearnerVocabularyProgress reconstitute(
            Long id,
            UUID userId,
            String hanzi,
            String pinyin,
            String translation,
            LanguageTag translationLanguage,
            LearnerVocabularyStatus status,
            Double masteryScore,
            Instant firstSeenAt,
            Instant lastReviewedAt,
            int reviewCount,
            Instant createdAt,
            Instant updatedAt) {
        return new LearnerVocabularyProgress(
                id,
                userId,
                hanzi,
                pinyin,
                translation,
                translationLanguage,
                status,
                masteryScore,
                firstSeenAt,
                lastReviewedAt,
                reviewCount,
                createdAt,
                updatedAt);
    }

    public LearnerVocabularyProgress refreshTranslation(String translation, Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        return new LearnerVocabularyProgress(
                id,
                userId,
                hanzi,
                pinyin,
                translation,
                translationLanguage,
                status,
                masteryScore,
                firstSeenAt,
                lastReviewedAt,
                reviewCount,
                createdAt,
                updatedAt);
    }

    public Long id() {
        return id;
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

    public LearnerVocabularyStatus status() {
        return status;
    }

    public Double masteryScore() {
        return masteryScore;
    }

    public Instant firstSeenAt() {
        return firstSeenAt;
    }

    public Instant lastReviewedAt() {
        return lastReviewedAt;
    }

    public int reviewCount() {
        return reviewCount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
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
