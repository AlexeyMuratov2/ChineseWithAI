package ru.chinesewithai.backend.agentruntime.application.command;

import java.util.Objects;

public record StartAgentSessionCommand(
        String profileKey,
        String modelKey,
        String task,
        String inputJson,
        String systemPromptAppendix,
        String workflowVariantKey) {

    public StartAgentSessionCommand {
        Objects.requireNonNull(profileKey, "profileKey must not be null");
        Objects.requireNonNull(modelKey, "modelKey must not be null");
        Objects.requireNonNull(task, "task must not be null");
        workflowVariantKey = normalizeOptional(workflowVariantKey);
    }

    public StartAgentSessionCommand(String profileKey, String modelKey, String task, String inputJson) {
        this(profileKey, modelKey, task, inputJson, null, null);
    }

    public StartAgentSessionCommand(
            String profileKey, String modelKey, String task, String inputJson, String systemPromptAppendix) {
        this(profileKey, modelKey, task, inputJson, systemPromptAppendix, null);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
