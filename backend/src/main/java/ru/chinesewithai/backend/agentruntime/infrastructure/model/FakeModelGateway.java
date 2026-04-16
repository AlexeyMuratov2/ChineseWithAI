package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageRole;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelResponse;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolCall;

@Component
public class FakeModelGateway implements AgentModelGateway {

    static final String MODEL_KEY = "fake-model";
    private static final String PROVIDER_KEY = "fake";
    private static final String STATIC_TOOL_NAME = "get_static_test_data";

    private final ObjectMapper objectMapper;

    public FakeModelGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerKey() {
        return PROVIDER_KEY;
    }

    @Override
    public List<AgentModelDescriptor> supportedModels() {
        return List.of(new AgentModelDescriptor(MODEL_KEY, "Fake Model", PROVIDER_KEY, false));
    }

    @Override
    public AgentModelResponse generate(AgentModelRequest request) {
        var toolMessage = request.messages().stream()
                .filter(message ->
                        message.role() == AgentModelMessageRole.TOOL && STATIC_TOOL_NAME.equals(message.name()))
                .findFirst()
                .map(message -> readJson(message.content()).path("toolMessage").asText(null))
                .orElse(null);

        if (toolMessage == null) {
            var toolCall = new AgentToolCall("fake-tool-call-1", STATIC_TOOL_NAME, "{}");
            var rawPayload = writeJson(Map.of(
                    "type", "TOOL_CALL",
                    "toolCalls", List.of(Map.of(
                            "id", toolCall.toolCallId(),
                            "toolName", toolCall.toolName(),
                            "arguments", Map.of()))));
            return AgentModelResponse.toolCalls(rawPayload, List.of(toolCall));
        }

        var finalOutput = writeJson(Map.of(
                "summary", "Fake agent completed successfully",
                "toolMessage", toolMessage));
        var rawPayload = writeJson(Map.of("type", "FINAL_OUTPUT", "output", readJson(finalOutput)));
        return AgentModelResponse.finalOutput(rawPayload, finalOutput);
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse fake model JSON", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize fake model payload", ex);
        }
    }
}
