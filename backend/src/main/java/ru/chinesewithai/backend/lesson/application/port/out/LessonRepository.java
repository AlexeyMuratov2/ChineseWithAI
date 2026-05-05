package ru.chinesewithai.backend.lesson.application.port.out;

import java.util.List;
import java.util.Optional;
import ru.chinesewithai.backend.lesson.domain.model.Lesson;
import ru.chinesewithai.backend.lesson.domain.model.LessonId;

public interface LessonRepository {
    Lesson save(Lesson lesson);

    Optional<Lesson> findById(LessonId lessonId);

    List<Lesson> findAllByModuleKeyOrderByCreatedAtDesc(String moduleKey);
}
