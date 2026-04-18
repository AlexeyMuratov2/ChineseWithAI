package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.lesson.application.port.out.LessonVocabularyItemRepository;
import ru.chinesewithai.backend.lesson.domain.model.LessonVocabularyItem;

@Repository
public class LessonVocabularyItemRepositoryJpaAdapter implements LessonVocabularyItemRepository {

    private final SpringDataLessonVocabularyItemJpaRepository repository;
    private final LessonVocabularyPersistenceMapper mapper;

    public LessonVocabularyItemRepositoryJpaAdapter(
            SpringDataLessonVocabularyItemJpaRepository repository, LessonVocabularyPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<LessonVocabularyItem> items) {
        repository.saveAll(items.stream().map(mapper::toEntity).toList());
    }
}
