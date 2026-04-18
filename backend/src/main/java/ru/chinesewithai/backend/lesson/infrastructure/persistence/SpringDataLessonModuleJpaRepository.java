package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLessonModuleJpaRepository extends JpaRepository<LessonModuleJpaEntity, String> {}
