package ru.chinesewithai.backend.lessondraft.domain.model;

import java.util.Objects;
import java.util.UUID;

public record LessonDraftId(UUID value) {

    public LessonDraftId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static LessonDraftId newId() {
        return new LessonDraftId(UUID.randomUUID());
    }

    public static LessonDraftId from(String value) {
        return new LessonDraftId(UUID.fromString(value));
    }
}
