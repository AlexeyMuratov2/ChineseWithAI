package ru.chinesewithai.backend.agentruntime.application.port.in;

import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.view.AgentSessionView;

public interface StartAgentSessionUseCase {
    AgentSessionView startSession(StartAgentSessionCommand command);
}
