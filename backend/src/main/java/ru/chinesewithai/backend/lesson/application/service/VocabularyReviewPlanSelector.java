package ru.chinesewithai.backend.lesson.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ru.chinesewithai.backend.lesson.application.port.out.LearnerVocabularyProgressRepository;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyProgress;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyStatus;
import ru.chinesewithai.backend.lesson.domain.model.SuggestedReviewMode;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewPlan;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewPlanItem;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewPolicy;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewReason;

@Service
public class VocabularyReviewPlanSelector {

    static final double DEFAULT_MASTERY_SCORE = 0.3d;
    private static final int MUST_REVIEW_LIMIT = 4;
    private static final int TOTAL_REVIEW_LIMIT = 10;
    private static final Set<LearnerVocabularyStatus> REVIEWABLE_STATUSES =
            EnumSet.of(LearnerVocabularyStatus.NEW, LearnerVocabularyStatus.LEARNING, LearnerVocabularyStatus.REVIEW);

    private final LearnerVocabularyProgressRepository learnerVocabularyProgressRepository;

    public VocabularyReviewPlanSelector(LearnerVocabularyProgressRepository learnerVocabularyProgressRepository) {
        this.learnerVocabularyProgressRepository = learnerVocabularyProgressRepository;
    }

    public VocabularyReviewPlan select(UUID userId, LanguageTag translationLanguage, Instant referenceNow) {
        var rankedCandidates = learnerVocabularyProgressRepository
                .findByUserIdAndTranslationLanguageAndStatusIn(userId, translationLanguage, REVIEWABLE_STATUSES)
                .stream()
                .filter(progress -> !wasReviewedTooRecently(progress, referenceNow))
                .map(progress -> new RankedReviewCandidate(
                        buildItem(progress),
                        calculatePriority(progress, referenceNow),
                        progress.firstSeenAt()))
                .sorted(Comparator.comparingDouble(RankedReviewCandidate::priority)
                        .reversed()
                        .thenComparing(RankedReviewCandidate::firstSeenAt)
                        .thenComparing(candidate -> candidate.item().hanzi())
                        .thenComparing(candidate -> candidate.item().pinyin()))
                .limit(TOTAL_REVIEW_LIMIT)
                .toList();

        if (rankedCandidates.isEmpty()) {
            return VocabularyReviewPlan.empty();
        }

        var mustReview = rankedCandidates.stream()
                .limit(MUST_REVIEW_LIMIT)
                .map(RankedReviewCandidate::item)
                .toList();
        var shouldReview = rankedCandidates.stream()
                .skip(MUST_REVIEW_LIMIT)
                .map(RankedReviewCandidate::item)
                .toList();
        return new VocabularyReviewPlan(mustReview, shouldReview, VocabularyReviewPolicy.DEFAULT);
    }

    private boolean wasReviewedTooRecently(LearnerVocabularyProgress progress, Instant referenceNow) {
        if (progress.lastReviewedAt() == null) {
            return false;
        }
        return progress.lastReviewedAt().isAfter(referenceNow.minus(2, ChronoUnit.DAYS));
    }

    private double calculatePriority(LearnerVocabularyProgress progress, Instant referenceNow) {
        var effectiveMasteryScore = effectiveMasteryScore(progress);
        var anchor = progress.lastReviewedAt() == null ? progress.firstSeenAt() : progress.lastReviewedAt();
        var daysSinceLastReview = Math.max(0L, ChronoUnit.DAYS.between(anchor, referenceNow));
        return daysSinceLastReview * (1.0d - effectiveMasteryScore);
    }

    private VocabularyReviewPlanItem buildItem(LearnerVocabularyProgress progress) {
        var effectiveMasteryScore = effectiveMasteryScore(progress);
        return new VocabularyReviewPlanItem(
                progress.hanzi(),
                progress.pinyin(),
                progress.translation(),
                effectiveMasteryScore,
                progress.lastReviewedAt(),
                determineReason(progress, effectiveMasteryScore),
                determineSuggestedReviewMode(progress.status(), effectiveMasteryScore));
    }

    private double effectiveMasteryScore(LearnerVocabularyProgress progress) {
        return progress.masteryScore() == null ? DEFAULT_MASTERY_SCORE : progress.masteryScore();
    }

    private VocabularyReviewReason determineReason(LearnerVocabularyProgress progress, double effectiveMasteryScore) {
        if (progress.lastReviewedAt() == null) {
            return VocabularyReviewReason.RECENTLY_LEARNED;
        }
        if (effectiveMasteryScore < 0.5d) {
            return VocabularyReviewReason.LOW_MASTERY;
        }
        return VocabularyReviewReason.OVERDUE;
    }

    private SuggestedReviewMode determineSuggestedReviewMode(
            LearnerVocabularyStatus status, double effectiveMasteryScore) {
        return switch (status) {
            case NEW -> SuggestedReviewMode.RECOGNITION;
            case LEARNING -> effectiveMasteryScore < 0.5d
                    ? SuggestedReviewMode.TRANSLATION
                    : SuggestedReviewMode.CONTEXT_READING;
            case REVIEW -> effectiveMasteryScore >= 0.7d
                    ? SuggestedReviewMode.PRODUCTION
                    : SuggestedReviewMode.CONTEXT_READING;
            case MASTERED, SUSPENDED -> SuggestedReviewMode.CONTEXT_READING;
        };
    }

    private record RankedReviewCandidate(VocabularyReviewPlanItem item, double priority, Instant firstSeenAt) {}
}
