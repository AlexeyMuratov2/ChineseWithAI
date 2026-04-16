package ru.chinesewithai.backend.agentruntime.application.command;

import java.util.Objects;
import java.util.UUID;

public record GetAgentSessionQuery(UUID sessionId) {

    public GetAgentSessionQuery {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
    }
}
