package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Objects;
import ru.chinesewithai.backend.agentruntime.domain.model.ModelResponseType;

public record AgentModelResponse(
        ModelResponseType responseType,
        String rawPayloadJson,
        List<AgentToolCall> toolCalls,
        String finalOutputJson) {

    public AgentModelResponse {
        Objects.requireNonNull(responseType, "responseType must not be null");
        Objects.requireNonNull(rawPayloadJson, "rawPayloadJson must not be null");
        switch (responseType) {
            case TOOL_CALL -> {
                Objects.requireNonNull(toolCalls, "toolCalls must not be null");
                toolCalls = List.copyOf(toolCalls);
                if (toolCalls.isEmpty()) {
                    throw new IllegalArgumentException("toolCalls must not be empty for tool call responses");
                }
                if (finalOutputJson != null) {
                    throw new IllegalArgumentException("finalOutputJson must be null for tool calls");
                }
            }
            case FINAL_OUTPUT -> {
                Objects.requireNonNull(finalOutputJson, "finalOutputJson must not be null");
                if (toolCalls != null) {
                    throw new IllegalArgumentException("toolCalls must be null for final outputs");
                }
            }
        }
    }

    public static AgentModelResponse toolCalls(String rawPayloadJson, List<AgentToolCall> toolCalls) {
        return new AgentModelResponse(ModelResponseType.TOOL_CALL, rawPayloadJson, toolCalls, null);
    }

    public static AgentModelResponse finalOutput(String rawPayloadJson, String finalOutputJson) {
        return new AgentModelResponse(ModelResponseType.FINAL_OUTPUT, rawPayloadJson, null, finalOutputJson);
    }
}
