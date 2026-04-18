package ru.chinesewithai.backend.lesson.domain.model;

import java.time.Instant;
import java.util.Objects;

public record LessonModule(
        String moduleKey,
        String displayName,
        String systemPromptAppendix,
        int schemaVersion,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public LessonModule {
        moduleKey = requireText(moduleKey, "moduleKey");
        displayName = requireText(displayName, "displayName");
        systemPromptAppendix = requireText(systemPromptAppendix, "systemPromptAppendix");
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be > 0");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
