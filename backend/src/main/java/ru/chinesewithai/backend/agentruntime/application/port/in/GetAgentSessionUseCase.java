package ru.chinesewithai.backend.agentruntime.application.port.in;

import ru.chinesewithai.backend.agentruntime.application.command.GetAgentSessionQuery;
import ru.chinesewithai.backend.agentruntime.application.view.AgentSessionView;

public interface GetAgentSessionUseCase {
    AgentSessionView getSession(GetAgentSessionQuery query);
}
