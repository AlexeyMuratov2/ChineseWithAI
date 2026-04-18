package ru.chinesewithai.backend.lesson.domain.model;

import java.time.Instant;
import java.util.Objects;

public record VocabularyReviewPlanItem(
        String hanzi,
        String pinyin,
        String translation,
        double masteryScore,
        Instant lastReviewedAt,
        VocabularyReviewReason reason,
        SuggestedReviewMode suggestedReviewMode) {

    public VocabularyReviewPlanItem {
        hanzi = requireText(hanzi, "hanzi");
        pinyin = requireText(pinyin, "pinyin");
        translation = requireText(translation, "translation");
        if (masteryScore < 0.0d || masteryScore > 1.0d) {
            throw new IllegalArgumentException("masteryScore must be between 0 and 1");
        }
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(suggestedReviewMode, "suggestedReviewMode must not be null");
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
