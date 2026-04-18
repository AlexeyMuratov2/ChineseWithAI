package ru.chinesewithai.backend.lesson.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.chinesewithai.backend.lesson.domain.model.Lesson;
import ru.chinesewithai.backend.lesson.domain.model.LessonId;

public interface LessonRepository {
    Lesson save(Lesson lesson);

    Optional<Lesson> findByIdAndOwnerId(LessonId lessonId, UUID ownerId);
}
