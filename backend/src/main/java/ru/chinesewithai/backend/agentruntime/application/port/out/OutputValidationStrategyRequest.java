package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public record OutputValidationStrategyRequest(
        String profileKey, String sessionInputJson, JsonNode output, String rawOutputJson) {

    public OutputValidationStrategyRequest {
        profileKey = requireText(profileKey, "profileKey");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(rawOutputJson, "rawOutputJson must not be null");
        sessionInputJson = normalizeOptional(sessionInputJson);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        var normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
