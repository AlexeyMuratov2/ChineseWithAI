package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;

public interface AgentContextBuilder {
    String key();

    List<AgentModelMessage> buildContext(AgentContextBuildRequest request);
}
