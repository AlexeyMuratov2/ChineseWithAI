package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLessonJpaRepository extends JpaRepository<LessonJpaEntity, UUID> {
    List<LessonJpaEntity> findAllByModuleKeyOrderByCreatedAtDesc(String moduleKey);
}
