package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_vocabulary_items")
public class LessonVocabularyItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "lesson_id", nullable = false, updatable = false)
    private UUID lessonId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "hanzi", nullable = false, length = 255)
    private String hanzi;

    @Column(name = "pinyin", nullable = false, length = 255)
    private String pinyin;

    @Column(name = "translation", nullable = false)
    private String translation;

    @Column(name = "translation_language", nullable = false, length = 35)
    private String translationLanguage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LessonVocabularyItemJpaEntity() {}

    public LessonVocabularyItemJpaEntity(
            Long id,
            UUID lessonId,
            UUID userId,
            String hanzi,
            String pinyin,
            String translation,
            String translationLanguage,
            Instant createdAt) {
        this.id = id;
        this.lessonId = lessonId;
        this.userId = userId;
        this.hanzi = hanzi;
        this.pinyin = pinyin;
        this.translation = translation;
        this.translationLanguage = translationLanguage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public UUID getUserId() {
        return userId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
