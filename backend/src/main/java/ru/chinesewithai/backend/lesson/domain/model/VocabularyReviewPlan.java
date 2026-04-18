package ru.chinesewithai.backend.lesson.domain.model;

import java.util.List;
import java.util.Objects;

public record VocabularyReviewPlan(
        List<VocabularyReviewPlanItem> mustReview, List<VocabularyReviewPlanItem> shouldReview, VocabularyReviewPolicy policy) {

    public VocabularyReviewPlan {
        Objects.requireNonNull(mustReview, "mustReview must not be null");
        Objects.requireNonNull(shouldReview, "shouldReview must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        mustReview = List.copyOf(mustReview);
        shouldReview = List.copyOf(shouldReview);
    }

    public static VocabularyReviewPlan empty() {
        return new VocabularyReviewPlan(List.of(), List.of(), VocabularyReviewPolicy.DEFAULT);
    }
}
