package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyProgress;
import ru.chinesewithai.backend.lesson.domain.model.LessonId;
import ru.chinesewithai.backend.lesson.domain.model.LessonVocabularyItem;

@Component
public class LessonVocabularyPersistenceMapper {

    public LessonVocabularyItemJpaEntity toEntity(LessonVocabularyItem item) {
        return new LessonVocabularyItemJpaEntity(
                item.id(),
                item.lessonId().value(),
                item.hanzi(),
                item.pinyin(),
                item.translation(),
                item.translationLanguage().value(),
                item.createdAt());
    }

    public LessonVocabularyItem toDomain(LessonVocabularyItemJpaEntity entity) {
        return LessonVocabularyItem.reconstitute(
                entity.getId(),
                new LessonId(entity.getLessonId()),
                entity.getHanzi(),
                entity.getPinyin(),
                entity.getTranslation(),
                LanguageTag.of(entity.getTranslationLanguage()),
                entity.getCreatedAt());
    }

    public LearnerVocabularyProgressJpaEntity toEntity(LearnerVocabularyProgress progress) {
        return new LearnerVocabularyProgressJpaEntity(
                progress.id(),
                progress.hanzi(),
                progress.pinyin(),
                progress.translation(),
                progress.translationLanguage().value(),
                progress.status(),
                progress.masteryScore(),
                progress.firstSeenAt(),
                progress.lastReviewedAt(),
                progress.reviewCount(),
                progress.createdAt(),
                progress.updatedAt());
    }

    public LearnerVocabularyProgress toDomain(LearnerVocabularyProgressJpaEntity entity) {
        return LearnerVocabularyProgress.reconstitute(
                entity.getId(),
                entity.getHanzi(),
                entity.getPinyin(),
                entity.getTranslation(),
                LanguageTag.of(entity.getTranslationLanguage()),
                entity.getStatus(),
                entity.getMasteryScore(),
                entity.getFirstSeenAt(),
                entity.getLastReviewedAt(),
                entity.getReviewCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
