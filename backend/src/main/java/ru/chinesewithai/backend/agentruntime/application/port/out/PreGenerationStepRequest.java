package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

public record PreGenerationStepRequest(
        AgentProfile profile, AgentSession session, JsonNode params, PreGenerationState state) {

    public PreGenerationStepRequest {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
