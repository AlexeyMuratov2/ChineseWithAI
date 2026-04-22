package ru.chinesewithai.backend.grammarexercise.application.exception;

import java.util.UUID;

public class GrammarExerciseGenerationFailedException extends RuntimeException {

    public GrammarExerciseGenerationFailedException(UUID sessionId, String reason) {
        super("Grammar exercise generation failed for session %s: %s".formatted(
                sessionId,
                reason == null || reason.isBlank() ? "unknown reason" : reason));
    }
}
