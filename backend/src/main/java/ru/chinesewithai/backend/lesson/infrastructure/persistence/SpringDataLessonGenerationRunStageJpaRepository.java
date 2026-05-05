package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLessonGenerationRunStageJpaRepository
        extends JpaRepository<LessonGenerationRunStageJpaEntity, UUID> {}
