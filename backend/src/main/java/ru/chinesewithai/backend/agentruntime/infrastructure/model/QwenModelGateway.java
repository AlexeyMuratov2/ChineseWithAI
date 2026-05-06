package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelCapability;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;

@Component
public class QwenModelGateway extends OpenAiCompatibleChatCompletionsGateway {

    static final String PROVIDER_KEY = "qwen";
    static final String MODEL_KEY = "qwen3.6-plus";

    private static final AgentModelDescriptor MODEL_DESCRIPTOR = new AgentModelDescriptor(
            MODEL_KEY,
            "Qwen3.6 Plus",
            PROVIDER_KEY,
            true,
            Set.of(
                    AgentModelCapability.TEXT_INPUT,
                    AgentModelCapability.IMAGE_INPUT,
                    AgentModelCapability.TOOL_CALLING,
                    AgentModelCapability.STRUCTURED_OUTPUT));

    private final QwenProperties properties;

    public QwenModelGateway(
            QwenProperties properties,
            AgentRuntimeProperties agentRuntimeProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        super(
                PROVIDER_KEY,
                "Qwen",
                "app.agentruntime.qwen.api-key / QWEN_API_KEY / DASHSCOPE_API_KEY",
                MODEL_DESCRIPTOR,
                properties.baseUrl(),
                properties.apiKey(),
                agentRuntimeProperties,
                restClientBuilder,
                objectMapper);
        this.properties = properties;
    }

    @Override
    protected Map<String, Object> additionalRequestParameters(AgentModelRequest request) {
        return Map.of("enable_thinking", properties.enableThinking());
    }
}
