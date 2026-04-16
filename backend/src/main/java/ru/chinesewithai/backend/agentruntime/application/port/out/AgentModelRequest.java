package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Objects;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

public record AgentModelRequest(
        AgentModelDescriptor model,
        AgentProfile profile,
        AgentSession session,
        List<AgentModelMessage> messages,
        List<AgentToolDefinition> tools) {

    public AgentModelRequest {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(messages, "messages must not be null");
        messages = List.copyOf(messages);
        Objects.requireNonNull(tools, "tools must not be null");
        tools = List.copyOf(tools);
    }
}
