package ru.chinesewithai.backend.agentruntime.application.port.out;

public enum AgentModelCapability {
    TEXT_INPUT("text_input"),
    IMAGE_INPUT("image_input"),
    TOOL_CALLING("tool_calling"),
    STRUCTURED_OUTPUT("structured_output");

    private final String apiName;

    AgentModelCapability(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
