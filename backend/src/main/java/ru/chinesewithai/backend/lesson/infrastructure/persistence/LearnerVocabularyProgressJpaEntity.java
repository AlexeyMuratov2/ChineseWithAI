package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyStatus;

@Entity
@Table(name = "learner_vocabulary_progress")
public class LearnerVocabularyProgressJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "hanzi", nullable = false, length = 255)
    private String hanzi;

    @Column(name = "pinyin", nullable = false, length = 255)
    private String pinyin;

    @Column(name = "translation", nullable = false)
    private String translation;

    @Column(name = "translation_language", nullable = false, length = 35)
    private String translationLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LearnerVocabularyStatus status;

    @Column(name = "mastery_score")
    private Double masteryScore;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LearnerVocabularyProgressJpaEntity() {}

    public LearnerVocabularyProgressJpaEntity(
            Long id,
            String hanzi,
            String pinyin,
            String translation,
            String translationLanguage,
            LearnerVocabularyStatus status,
            Double masteryScore,
            Instant firstSeenAt,
            Instant lastReviewedAt,
            int reviewCount,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.hanzi = hanzi;
        this.pinyin = pinyin;
        this.translation = translation;
        this.translationLanguage = translationLanguage;
        this.status = status;
        this.masteryScore = masteryScore;
        this.firstSeenAt = firstSeenAt;
        this.lastReviewedAt = lastReviewedAt;
        this.reviewCount = reviewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getHanzi() {
        return hanzi;
    }

    public String getPinyin() {
        return pinyin;
    }

    public String getTranslation() {
        return translation;
    }

    public String getTranslationLanguage() {
        return translationLanguage;
    }

    public LearnerVocabularyStatus getStatus() {
        return status;
    }

    public Double getMasteryScore() {
        return masteryScore;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
