package ru.chinesewithai.backend.agentruntime.domain.model;

public enum AgentStepType {
    SESSION_CREATED,
    CONTEXT_BUILT,
    MODEL_REQUEST,
    MODEL_RESPONSE,
    TOOL_CALL,
    TOOL_RESULT,
    OUTPUT_VALIDATION_FAILED,
    FINAL_OUTPUT,
    SESSION_COMPLETED,
    SESSION_FAILED
}
