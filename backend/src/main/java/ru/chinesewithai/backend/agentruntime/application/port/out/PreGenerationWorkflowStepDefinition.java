package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Objects;

public record PreGenerationWorkflowStepDefinition(String stepKey, boolean enabled, JsonNode params) {

    public PreGenerationWorkflowStepDefinition {
        stepKey = requireText(stepKey, "stepKey");
        params = params == null ? JsonNodeFactory.instance.objectNode() : params;
        if (!params.isObject()) {
            throw new IllegalArgumentException("Pre-generation step params must be a JSON object");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
