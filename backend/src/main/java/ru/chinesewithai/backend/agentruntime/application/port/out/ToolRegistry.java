package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Optional;

public interface ToolRegistry {
    Optional<AgentTool> findByName(String toolName);

    boolean isRegistered(String toolName);

    List<AgentToolDefinition> getDefinitions(List<String> toolNames);
}
