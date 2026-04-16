package ru.chinesewithai.backend.agentruntime.domain.model;

public enum AgentStepType {
    SESSION_CREATED,
    CONTEXT_BUILT,
    MODEL_REQUEST,
    MODEL_RESPONSE,
    TOOL_CALL,
    TOOL_RESULT,
    FINAL_OUTPUT,
    SESSION_COMPLETED,
    SESSION_FAILED
}
