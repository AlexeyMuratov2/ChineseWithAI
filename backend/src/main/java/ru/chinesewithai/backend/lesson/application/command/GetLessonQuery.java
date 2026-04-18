package ru.chinesewithai.backend.lesson.application.command;

import java.util.Objects;
import java.util.UUID;

public record GetLessonQuery(UUID lessonId) {

    public GetLessonQuery {
        Objects.requireNonNull(lessonId, "lessonId must not be null");
    }
}
