package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.lesson.application.port.out.LessonModuleRepository;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

@Repository
public class LessonModuleRepositoryJpaAdapter implements LessonModuleRepository {

    private final SpringDataLessonModuleJpaRepository repository;
    private final LessonPersistenceMapper mapper;

    public LessonModuleRepositoryJpaAdapter(
            SpringDataLessonModuleJpaRepository repository, LessonPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<LessonModule> findByModuleKey(String moduleKey) {
        return repository.findById(moduleKey).map(mapper::toDomain);
    }

    @Override
    public List<LessonModule> findAllOrderByModuleKeyAsc() {
        return repository.findAllByOrderByModuleKeyAsc().stream().map(mapper::toDomain).toList();
    }
}
