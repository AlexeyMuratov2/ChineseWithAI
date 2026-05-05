package ru.chinesewithai.backend.lesson.application.generation;

import java.util.Objects;
import java.util.UUID;

public record LessonGenerationPipelineResult(String finalOutputJson, UUID finalGeneratorSessionId, UUID generationRunId) {

    public LessonGenerationPipelineResult {
        finalOutputJson = requireText(finalOutputJson, "finalOutputJson");
        Objects.requireNonNull(finalGeneratorSessionId, "finalGeneratorSessionId must not be null");
        Objects.requireNonNull(generationRunId, "generationRunId must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
