package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.lesson.application.port.out.LessonRepository;
import ru.chinesewithai.backend.lesson.domain.model.Lesson;
import ru.chinesewithai.backend.lesson.domain.model.LessonId;

@Repository
public class LessonRepositoryJpaAdapter implements LessonRepository {

    private final SpringDataLessonJpaRepository repository;
    private final LessonPersistenceMapper mapper;

    public LessonRepositoryJpaAdapter(SpringDataLessonJpaRepository repository, LessonPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Lesson save(Lesson lesson) {
        return mapper.toDomain(repository.save(mapper.toEntity(lesson)));
    }

    @Override
    public Optional<Lesson> findById(LessonId lessonId) {
        return repository.findById(lessonId.value()).map(mapper::toDomain);
    }

    @Override
    public List<Lesson> findAllByModuleKeyOrderByCreatedAtDesc(String moduleKey) {
        return repository.findAllByModuleKeyOrderByCreatedAtDesc(moduleKey).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
