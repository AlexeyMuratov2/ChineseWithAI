package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelCapability;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;

@Component
public class DeepSeekModelGateway extends OpenAiCompatibleChatCompletionsGateway {

    static final String PROVIDER_KEY = "deepseek";
    static final String MODEL_KEY = "deepseek-chat";

    private static final AgentModelDescriptor MODEL_DESCRIPTOR = new AgentModelDescriptor(
            MODEL_KEY,
            "DeepSeek Chat",
            PROVIDER_KEY,
            true,
            Set.of(AgentModelCapability.TEXT_INPUT, AgentModelCapability.TOOL_CALLING));

    public DeepSeekModelGateway(
            DeepSeekProperties properties,
            AgentRuntimeProperties agentRuntimeProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        super(
                PROVIDER_KEY,
                "DeepSeek",
                "app.agentruntime.deepseek.api-key / DEEPSEEK_API_KEY",
                MODEL_DESCRIPTOR,
                properties.baseUrl(),
                properties.apiKey(),
                agentRuntimeProperties,
                restClientBuilder,
                objectMapper);
    }
}
