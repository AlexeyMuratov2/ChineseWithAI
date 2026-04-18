package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyStatus;

interface SpringDataLearnerVocabularyProgressJpaRepository
        extends JpaRepository<LearnerVocabularyProgressJpaEntity, Long> {

    Optional<LearnerVocabularyProgressJpaEntity> findByUserIdAndHanziAndPinyinAndTranslationLanguage(
            UUID userId, String hanzi, String pinyin, String translationLanguage);

    List<LearnerVocabularyProgressJpaEntity> findByUserIdAndTranslationLanguageAndStatusIn(
            UUID userId, String translationLanguage, Collection<LearnerVocabularyStatus> statuses);
}
