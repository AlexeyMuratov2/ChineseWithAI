package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

public record OutputValidationStrategyRequest(
        AgentProfile profile, AgentSession session, JsonNode output, String rawOutputJson) {

    public OutputValidationStrategyRequest {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(rawOutputJson, "rawOutputJson must not be null");
    }
}
