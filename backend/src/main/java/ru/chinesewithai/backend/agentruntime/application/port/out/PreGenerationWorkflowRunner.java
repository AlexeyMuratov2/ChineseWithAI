package ru.chinesewithai.backend.agentruntime.application.port.out;

import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

public interface PreGenerationWorkflowRunner {
    PreGenerationWorkflowExecutionResult run(AgentProfile profile, AgentSession session);
}
