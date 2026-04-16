package ru.chinesewithai.backend.agentruntime.domain.model;

import java.util.List;
import java.util.Objects;

public record AgentProfile(
        String profileKey,
        String displayName,
        String systemPrompt,
        String contextBuilderKey,
        List<String> allowedToolNames,
        ExecutionPolicy executionPolicy,
        MemoryPolicy memoryPolicy,
        OutputContract outputContract,
        boolean visible) {

    public AgentProfile {
        profileKey = requireText(profileKey, "profileKey");
        displayName = requireText(displayName, "displayName");
        systemPrompt = requireText(systemPrompt, "systemPrompt");
        contextBuilderKey = requireText(contextBuilderKey, "contextBuilderKey");
        Objects.requireNonNull(allowedToolNames, "allowedToolNames must not be null");
        allowedToolNames = List.copyOf(allowedToolNames);
        Objects.requireNonNull(executionPolicy, "executionPolicy must not be null");
        Objects.requireNonNull(memoryPolicy, "memoryPolicy must not be null");
        Objects.requireNonNull(outputContract, "outputContract must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
