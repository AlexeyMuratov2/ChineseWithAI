package ru.chinesewithai.backend.lesson.domain.model;

import java.util.Objects;
import java.util.UUID;

public record LessonId(UUID value) {

    public LessonId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static LessonId newId() {
        return new LessonId(UUID.randomUUID());
    }
}
