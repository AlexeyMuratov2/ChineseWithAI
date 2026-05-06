package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Objects;

public record AgentModelMessage(
        AgentModelMessageRole role,
        String name,
        String content,
        List<AgentModelMessageContentPart> contentParts,
        String toolCallId,
        List<AgentToolCall> toolCalls) {

    public AgentModelMessage(
            AgentModelMessageRole role, String name, String content, String toolCallId, List<AgentToolCall> toolCalls) {
        this(role, name, content, null, toolCallId, toolCalls);
    }

    public AgentModelMessage {
        Objects.requireNonNull(role, "role must not be null");
        contentParts = contentParts == null ? null : List.copyOf(contentParts);
        toolCalls = toolCalls == null ? null : List.copyOf(toolCalls);
        switch (role) {
            case SYSTEM -> {
                requireContent(content);
                requireNull(contentParts, "contentParts");
                requireNull(name, "name");
                requireNull(toolCallId, "toolCallId");
                requireNull(toolCalls, "toolCalls");
            }
            case USER -> {
                requireUserContent(content, contentParts);
                requireNull(name, "name");
                requireNull(toolCallId, "toolCallId");
                requireNull(toolCalls, "toolCalls");
            }
            case ASSISTANT -> {
                requireNull(name, "name");
                requireNull(contentParts, "contentParts");
                requireNull(toolCallId, "toolCallId");
                if ((content == null || content.isBlank()) && (toolCalls == null || toolCalls.isEmpty())) {
                    throw new IllegalArgumentException("Assistant message must have content or tool calls");
                }
                if (toolCalls != null && toolCalls.isEmpty()) {
                    throw new IllegalArgumentException("Assistant tool calls must not be empty");
                }
            }
            case TOOL -> {
                requireContent(content);
                requireNull(contentParts, "contentParts");
                requireText(name, "name");
                requireText(toolCallId, "toolCallId");
                requireNull(toolCalls, "toolCalls");
            }
        }
    }

    public static AgentModelMessage system(String content) {
        return new AgentModelMessage(AgentModelMessageRole.SYSTEM, null, content, null, null);
    }

    public static AgentModelMessage user(String content) {
        return new AgentModelMessage(AgentModelMessageRole.USER, null, content, null, null);
    }

    public static AgentModelMessage user(List<AgentModelMessageContentPart> contentParts) {
        return new AgentModelMessage(AgentModelMessageRole.USER, null, null, contentParts, null, null);
    }

    public static AgentModelMessage assistant(String content) {
        return new AgentModelMessage(AgentModelMessageRole.ASSISTANT, null, content, null, null);
    }

    public static AgentModelMessage assistantToolCalls(List<AgentToolCall> toolCalls) {
        return new AgentModelMessage(AgentModelMessageRole.ASSISTANT, null, null, null, toolCalls);
    }

    public static AgentModelMessage tool(String toolCallId, String toolName, String content) {
        return new AgentModelMessage(AgentModelMessageRole.TOOL, toolName, content, toolCallId, null);
    }

    private static void requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    private static void requireUserContent(String content, List<AgentModelMessageContentPart> contentParts) {
        var hasTextContent = content != null && !content.isBlank();
        var hasContentParts = contentParts != null && !contentParts.isEmpty();
        if (hasTextContent == hasContentParts) {
            throw new IllegalArgumentException("User message must have either content or contentParts");
        }
    }

    private static void requireNull(Object value, String fieldName) {
        if (value != null) {
            throw new IllegalArgumentException(fieldName + " must be null");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
