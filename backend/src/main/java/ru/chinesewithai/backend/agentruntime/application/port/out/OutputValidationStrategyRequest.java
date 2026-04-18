package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;

public record OutputValidationStrategyRequest(
        AgentProfile profile, String sessionInputJson, JsonNode output, String rawOutputJson) {

    public OutputValidationStrategyRequest {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(rawOutputJson, "rawOutputJson must not be null");
        sessionInputJson = normalizeOptional(sessionInputJson);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
