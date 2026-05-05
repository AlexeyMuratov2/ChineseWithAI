package ru.chinesewithai.backend.lesson.domain.model;

import java.time.Instant;
import java.util.Objects;

public record LessonModule(
        String moduleKey,
        String displayName,
        String systemPromptAppendix,
        int schemaVersion,
        boolean active,
        String generatorProfileKey,
        String generatorWorkflowVariantKey,
        String generationPipelineKey,
        Instant createdAt,
        Instant updatedAt) {

    public LessonModule {
        moduleKey = requireText(moduleKey, "moduleKey");
        displayName = requireText(displayName, "displayName");
        systemPromptAppendix = requireText(systemPromptAppendix, "systemPromptAppendix");
        generatorProfileKey = requireText(generatorProfileKey, "generatorProfileKey");
        generatorWorkflowVariantKey = normalizeOptional(generatorWorkflowVariantKey);
        generationPipelineKey = normalizeOptional(generationPipelineKey);
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

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
