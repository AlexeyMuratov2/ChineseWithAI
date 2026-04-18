package ru.chinesewithai.backend.lesson.application.exception;

import java.util.UUID;

public class LessonGenerationFailedException extends RuntimeException {

    public LessonGenerationFailedException(UUID sessionId, String reason) {
        super("Lesson generation failed for session %s: %s".formatted(
                sessionId,
                reason == null || reason.isBlank() ? "unknown reason" : reason));
    }
}
