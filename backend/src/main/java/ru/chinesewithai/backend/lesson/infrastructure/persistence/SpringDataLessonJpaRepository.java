package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLessonJpaRepository extends JpaRepository<LessonJpaEntity, UUID> {
    Optional<LessonJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
