package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.lesson.application.port.out.LearnerVocabularyProgressRepository;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyProgress;
import ru.chinesewithai.backend.lesson.domain.model.LearnerVocabularyStatus;

@Repository
public class LearnerVocabularyProgressRepositoryJpaAdapter implements LearnerVocabularyProgressRepository {

    private final SpringDataLearnerVocabularyProgressJpaRepository repository;
    private final LessonVocabularyPersistenceMapper mapper;

    public LearnerVocabularyProgressRepositoryJpaAdapter(
            SpringDataLearnerVocabularyProgressJpaRepository repository, LessonVocabularyPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<LearnerVocabularyProgress> findByHanziAndPinyinAndTranslationLanguage(
            String hanzi, String pinyin, LanguageTag translationLanguage) {
        return repository.findFirstByHanziAndPinyinAndTranslationLanguageOrderByUpdatedAtDescIdDesc(
                        hanzi, pinyin, translationLanguage.value())
                .map(mapper::toDomain);
    }

    @Override
    public LearnerVocabularyProgress save(LearnerVocabularyProgress progress) {
        return mapper.toDomain(repository.save(mapper.toEntity(progress)));
    }

    @Override
    public List<LearnerVocabularyProgress> findByTranslationLanguageAndStatusIn(
            LanguageTag translationLanguage, Set<LearnerVocabularyStatus> statuses) {
        return repository.findByTranslationLanguageAndStatusIn(translationLanguage.value(), statuses).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
