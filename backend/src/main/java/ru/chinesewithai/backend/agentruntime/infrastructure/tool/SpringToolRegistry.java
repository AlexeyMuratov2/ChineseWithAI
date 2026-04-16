package ru.chinesewithai.backend.agentruntime.infrastructure.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentTool;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolDefinition;
import ru.chinesewithai.backend.agentruntime.application.port.out.ToolRegistry;

@Component
public class SpringToolRegistry implements ToolRegistry {

    private final Map<String, AgentTool> toolsByName;

    public SpringToolRegistry(List<AgentTool> tools) {
        var indexed = new LinkedHashMap<String, AgentTool>();
        for (var tool : tools) {
            var previous = indexed.put(tool.name(), tool);
            if (previous != null) {
                throw new IllegalStateException("Duplicate tool name: " + tool.name());
            }
        }
        this.toolsByName = Map.copyOf(indexed);
    }

    @Override
    public Optional<AgentTool> findByName(String toolName) {
        return Optional.ofNullable(toolsByName.get(toolName));
    }

    @Override
    public boolean isRegistered(String toolName) {
        return toolsByName.containsKey(toolName);
    }

    @Override
    public List<AgentToolDefinition> getDefinitions(List<String> toolNames) {
        return toolNames.stream()
                .map(this::getRequired)
                .map(tool -> new AgentToolDefinition(tool.name(), tool.description(), tool.inputSchemaJson()))
                .toList();
    }

    private AgentTool getRequired(String toolName) {
        var tool = toolsByName.get(toolName);
        if (tool == null) {
            throw new IllegalStateException("Unknown tool: " + toolName);
        }
        return tool;
    }
}
