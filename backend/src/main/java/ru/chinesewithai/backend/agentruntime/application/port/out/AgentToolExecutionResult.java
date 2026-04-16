package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;

public record AgentToolExecutionResult(String resultJson) {

    public AgentToolExecutionResult {
        Objects.requireNonNull(resultJson, "resultJson must not be null");
    }
}
