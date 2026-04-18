package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Objects;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

public record AgentContextBuildRequest(
        AgentProfile profile,
        AgentSession session,
        List<AgentModelMessage> conversationHistory,
        PreGenerationState preGenerationState) {

    public AgentContextBuildRequest {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(conversationHistory, "conversationHistory must not be null");
        Objects.requireNonNull(preGenerationState, "preGenerationState must not be null");
        conversationHistory = List.copyOf(conversationHistory);
    }

    public AgentContextBuildRequest(AgentProfile profile, AgentSession session, List<AgentModelMessage> conversationHistory) {
        this(profile, session, conversationHistory, PreGenerationState.empty());
    }
}
