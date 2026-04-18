package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLessonModuleJpaRepository extends JpaRepository<LessonModuleJpaEntity, String> {

    List<LessonModuleJpaEntity> findAllByOrderByModuleKeyAsc();
}
