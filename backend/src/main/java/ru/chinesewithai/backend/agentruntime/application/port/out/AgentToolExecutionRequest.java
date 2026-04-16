package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

public record AgentToolExecutionRequest(AgentProfile profile, AgentSession session, String argumentsJson) {

    public AgentToolExecutionRequest {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(argumentsJson, "argumentsJson must not be null");
    }
}
