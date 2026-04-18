package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Objects;

public record PreGenerationWorkflow(
        String profileKey, String workflowVariantKey, List<PreGenerationWorkflowStepDefinition> steps) {

    public PreGenerationWorkflow {
        profileKey = requireText(profileKey, "profileKey");
        workflowVariantKey = normalizeOptional(workflowVariantKey);
        Objects.requireNonNull(steps, "steps must not be null");
        steps = List.copyOf(steps);
    }

    public static PreGenerationWorkflow empty(String profileKey) {
        return new PreGenerationWorkflow(profileKey, null, List.of());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
