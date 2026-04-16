package ru.chinesewithai.backend.agentruntime.application.port.out;

public interface AgentTool {
    String name();

    String description();

    String inputSchemaJson();

    AgentToolExecutionResult execute(AgentToolExecutionRequest request);
}
