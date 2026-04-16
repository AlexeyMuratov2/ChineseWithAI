package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;

public record AgentToolCall(String toolCallId, String toolName, String argumentsJson) {

    public AgentToolCall {
        Objects.requireNonNull(toolCallId, "toolCallId must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(argumentsJson, "argumentsJson must not be null");
        if (toolCallId.isBlank()) {
            throw new IllegalArgumentException("toolCallId must not be blank");
        }
    }
}
