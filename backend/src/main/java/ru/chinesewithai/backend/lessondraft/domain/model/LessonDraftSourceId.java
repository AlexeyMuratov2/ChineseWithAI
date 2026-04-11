package ru.chinesewithai.backend.lessondraft.domain.model;

import java.util.Objects;
import java.util.UUID;

public record LessonDraftSourceId(UUID value) {

    public LessonDraftSourceId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static LessonDraftSourceId newId() {
        return new LessonDraftSourceId(UUID.randomUUID());
    }

    public static LessonDraftSourceId from(String value) {
        return new LessonDraftSourceId(UUID.fromString(value));
    }
}
