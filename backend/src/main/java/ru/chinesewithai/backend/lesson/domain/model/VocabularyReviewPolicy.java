package ru.chinesewithai.backend.lesson.domain.model;

public record VocabularyReviewPolicy(
        int mustReviewMinOccurrences, int shouldReviewMinOccurrences, boolean mustReviewMustAppearInExercise) {

    public static final VocabularyReviewPolicy DEFAULT = new VocabularyReviewPolicy(2, 1, true);

    public VocabularyReviewPolicy {
        if (mustReviewMinOccurrences <= 0) {
            throw new IllegalArgumentException("mustReviewMinOccurrences must be > 0");
        }
        if (shouldReviewMinOccurrences <= 0) {
            throw new IllegalArgumentException("shouldReviewMinOccurrences must be > 0");
        }
    }
}
