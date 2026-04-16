package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

public interface AgentSessionRepository {
    AgentSession save(AgentSession session);

    Optional<AgentSession> findByIdAndOwnerId(UUID sessionId, UUID ownerId);
}
