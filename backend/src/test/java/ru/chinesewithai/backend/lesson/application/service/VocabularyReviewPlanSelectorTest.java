package ru.chinesewithai.backend.lesson.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.application.port.out.LearnerVocabularyProgressRepository;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyProgress;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyStatus;

class VocabularyReviewPlanSelectorTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LanguageTag TRANSLATION_LANGUAGE = LanguageTag.of("en");
    private static final Instant NOW = Instant.parse("2026-04-18T10:15:30Z");

    @Test
    void lowMasteryAndOverdueWordFallsIntoMustReview() {
        var selector = new VocabularyReviewPlanSelector(new StubLearnerVocabularyProgressRepository(List.of(
                progress(
                        "认识",
                        "rènshi",
                        "to know",
                        LearnerVocabularyStatus.LEARNING,
                        0.1d,
                        Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-05T00:00:00Z")),
                progress(
                        "学习",
                        "xuéxí",
                        "to study",
                        LearnerVocabularyStatus.REVIEW,
                        0.9d,
                        Instant.parse("2026-04-02T00:00:00Z"),
                        Instant.parse("2026-04-16T00:00:00Z")))));

        var plan = selector.select(USER_ID, TRANSLATION_LANGUAGE, NOW);

        assertThat(plan.mustReview()).extracting(item -> item.hanzi()).contains("认识");
    }

    @Test
    void wordReviewedYesterdayIsExcluded() {
        var selector = new VocabularyReviewPlanSelector(new StubLearnerVocabularyProgressRepository(List.of(
                progress(
                        "认识",
                        "rènshi",
                        "to know",
                        LearnerVocabularyStatus.LEARNING,
                        0.2d,
                        Instant.parse("2026-04-01T00:00:00Z"),
                        NOW.minusSeconds(24L * 60L * 60L)),
                progress(
                        "学习",
                        "xuéxí",
                        "to study",
                        LearnerVocabularyStatus.LEARNING,
                        0.4d,
                        Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-10T00:00:00Z")))));

        var plan = selector.select(USER_ID, TRANSLATION_LANGUAGE, NOW);

        assertThat(plan.mustReview()).extracting(item -> item.hanzi()).doesNotContain("认识");
        assertThat(plan.shouldReview()).extracting(item -> item.hanzi()).doesNotContain("认识");
    }

    @Test
    void nullMasteryScoreUsesEffectiveScorePointThree() {
        var selector = new VocabularyReviewPlanSelector(new StubLearnerVocabularyProgressRepository(List.of(progress(
                "认识",
                "rènshi",
                "to know",
                LearnerVocabularyStatus.NEW,
                null,
                Instant.parse("2026-04-10T00:00:00Z"),
                null))));

        var plan = selector.select(USER_ID, TRANSLATION_LANGUAGE, NOW);

        assertThat(plan.mustReview()).singleElement().extracting(item -> item.masteryScore()).isEqualTo(0.3d);
    }

    @Test
    void returnsSmallerPlanWhenFewerThanTenWordsMatch() {
        var selector = new VocabularyReviewPlanSelector(new StubLearnerVocabularyProgressRepository(List.of(
                progress("一", "yī", "one", LearnerVocabularyStatus.NEW, 0.2d, Instant.parse("2026-04-01T00:00:00Z"), null),
                progress("二", "èr", "two", LearnerVocabularyStatus.NEW, 0.2d, Instant.parse("2026-04-02T00:00:00Z"), null),
                progress("三", "sān", "three", LearnerVocabularyStatus.REVIEW, 0.8d, Instant.parse("2026-04-03T00:00:00Z"),
                        Instant.parse("2026-04-10T00:00:00Z")))));

        var plan = selector.select(USER_ID, TRANSLATION_LANGUAGE, NOW);

        assertThat(plan.mustReview()).hasSize(3);
        assertThat(plan.shouldReview()).isEmpty();
    }

    @Test
    void suspendedAndMasteredWordsAreExcluded() {
        var selector = new VocabularyReviewPlanSelector(new StubLearnerVocabularyProgressRepository(List.of(
                progress("一", "yī", "one", LearnerVocabularyStatus.SUSPENDED, 0.1d, Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-02T00:00:00Z")),
                progress("二", "èr", "two", LearnerVocabularyStatus.MASTERED, 0.9d, Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-02T00:00:00Z")),
                progress("三", "sān", "three", LearnerVocabularyStatus.REVIEW, 0.4d, Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-10T00:00:00Z")))));

        var plan = selector.select(USER_ID, TRANSLATION_LANGUAGE, NOW);

        assertThat(plan.mustReview()).extracting(item -> item.hanzi()).containsExactly("三");
        assertThat(plan.shouldReview()).isEmpty();
    }

    @Test
    void appliesMustReviewAndShouldReviewLimits() {
        var selector = new VocabularyReviewPlanSelector(new StubLearnerVocabularyProgressRepository(List.of(
                progress("一", "yī", "one", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-01T00:00:00Z"),
                        Instant.parse("2026-03-02T00:00:00Z")),
                progress("二", "èr", "two", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-02T00:00:00Z"),
                        Instant.parse("2026-03-03T00:00:00Z")),
                progress("三", "sān", "three", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-03T00:00:00Z"),
                        Instant.parse("2026-03-04T00:00:00Z")),
                progress("四", "sì", "four", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-04T00:00:00Z"),
                        Instant.parse("2026-03-05T00:00:00Z")),
                progress("五", "wǔ", "five", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-05T00:00:00Z"),
                        Instant.parse("2026-03-06T00:00:00Z")),
                progress("六", "liù", "six", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-06T00:00:00Z"),
                        Instant.parse("2026-03-07T00:00:00Z")),
                progress("七", "qī", "seven", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-07T00:00:00Z"),
                        Instant.parse("2026-03-08T00:00:00Z")),
                progress("八", "bā", "eight", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-08T00:00:00Z"),
                        Instant.parse("2026-03-09T00:00:00Z")),
                progress("九", "jiǔ", "nine", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-09T00:00:00Z"),
                        Instant.parse("2026-03-10T00:00:00Z")),
                progress("十", "shí", "ten", LearnerVocabularyStatus.LEARNING, 0.1d, Instant.parse("2026-03-10T00:00:00Z"),
                        Instant.parse("2026-03-11T00:00:00Z")),
                progress("十一", "shíyī", "eleven", LearnerVocabularyStatus.LEARNING, 0.1d,
                        Instant.parse("2026-03-11T00:00:00Z"), Instant.parse("2026-03-12T00:00:00Z")),
                progress("十二", "shí’èr", "twelve", LearnerVocabularyStatus.LEARNING, 0.1d,
                        Instant.parse("2026-03-12T00:00:00Z"), Instant.parse("2026-03-13T00:00:00Z")))));

        var plan = selector.select(USER_ID, TRANSLATION_LANGUAGE, NOW);

        assertThat(plan.mustReview()).hasSize(4);
        assertThat(plan.shouldReview()).hasSize(6);
    }

    private LearnerVocabularyProgress progress(
            String hanzi,
            String pinyin,
            String translation,
            LearnerVocabularyStatus status,
            Double masteryScore,
            Instant firstSeenAt,
            Instant lastReviewedAt) {
        return LearnerVocabularyProgress.reconstitute(
                null,
                USER_ID,
                hanzi,
                pinyin,
                translation,
                TRANSLATION_LANGUAGE,
                status,
                masteryScore,
                firstSeenAt,
                lastReviewedAt,
                0,
                firstSeenAt,
                firstSeenAt);
    }

    private record StubLearnerVocabularyProgressRepository(List<LearnerVocabularyProgress> items)
            implements LearnerVocabularyProgressRepository {

        @Override
        public Optional<LearnerVocabularyProgress> findByUserIdAndHanziAndPinyinAndTranslationLanguage(
                UUID userId, String hanzi, String pinyin, LanguageTag translationLanguage) {
            return items.stream()
                    .filter(item -> item.userId().equals(userId))
                    .filter(item -> item.hanzi().equals(hanzi))
                    .filter(item -> item.pinyin().equals(pinyin))
                    .filter(item -> item.translationLanguage().equals(translationLanguage))
                    .findFirst();
        }

        @Override
        public LearnerVocabularyProgress save(LearnerVocabularyProgress progress) {
            return progress;
        }

        @Override
        public List<LearnerVocabularyProgress> findByUserIdAndTranslationLanguageAndStatusIn(
                UUID userId, LanguageTag translationLanguage, Set<LearnerVocabularyStatus> statuses) {
            return items.stream()
                    .filter(item -> item.userId().equals(userId))
                    .filter(item -> item.translationLanguage().equals(translationLanguage))
                    .filter(item -> statuses.contains(item.status()))
                    .toList();
        }
    }
}
