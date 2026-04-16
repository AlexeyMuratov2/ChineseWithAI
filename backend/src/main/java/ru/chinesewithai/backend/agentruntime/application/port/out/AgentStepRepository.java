package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.UUID;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStep;

public interface AgentStepRepository {
    AgentStep save(AgentStep step);

    List<AgentStep> findBySessionIdOrderByStepIndex(UUID sessionId);
}
