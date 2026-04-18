package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLessonVocabularyItemJpaRepository extends JpaRepository<LessonVocabularyItemJpaEntity, Long> {}
