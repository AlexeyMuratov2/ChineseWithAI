package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageRole;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelResponse;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolCall;

@Component
public class DeepSeekModelGateway implements AgentModelGateway {

    static final String PROVIDER_KEY = "deepseek";
    static final String MODEL_KEY = "deepseek-chat";
    private static final Logger log = LoggerFactory.getLogger(DeepSeekModelGateway.class);

    private static final AgentModelDescriptor MODEL_DESCRIPTOR =
            new AgentModelDescriptor(MODEL_KEY, "DeepSeek Chat", PROVIDER_KEY, true);

    private final DeepSeekProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DeepSeekModelGateway(DeepSeekProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + safeApiKey(properties.apiKey()))
                .build();
        this.objectMapper = objectMapper;

        if (isApiKeyMissing()) {
            log.warn(
                    "DeepSeek model gateway is disabled because app.agentruntime.deepseek.api-key / DEEPSEEK_API_KEY is blank");
        } else {
            log.info("DeepSeek model gateway is enabled with base URL {}", properties.baseUrl());
        }
    }

    @Override
    public String providerKey() {
        return PROVIDER_KEY;
    }

    @Override
    public List<AgentModelDescriptor> supportedModels() {
        if (isApiKeyMissing()) {
            return List.of();
        }
        return List.of(MODEL_DESCRIPTOR);
    }

    @Override
    public AgentModelResponse generate(AgentModelRequest request) {
        if (isApiKeyMissing()) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }

        try {
            var response = restClient
                    .post()
                    .uri("/chat/completions")
                    .body(buildRequestBody(request))
                    .retrieve()
                    .body(DeepSeekChatCompletionResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("DeepSeek returned no choices");
            }

            var choice = response.choices().getFirst();
            var message = choice.message();
            var rawPayloadJson = writeJson(response);

            if (message != null && message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                var toolCalls = message.toolCalls().stream()
                        .map(toolCall -> new AgentToolCall(
                                requireText(toolCall.id(), "toolCall.id"),
                                requireText(toolCall.function().name(), "toolCall.function.name"),
                                defaultArguments(toolCall.function().arguments())))
                        .toList();
                return AgentModelResponse.toolCalls(rawPayloadJson, toolCalls);
            }

            if (message == null || message.content() == null || message.content().isBlank()) {
                throw new IllegalStateException("DeepSeek returned empty final content");
            }

            return AgentModelResponse.finalOutput(rawPayloadJson, normalizeFinalOutput(message.content()));
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "DeepSeek API request failed with status "
                            + ex.getStatusCode().value()
                            + ": "
                            + responseBody(ex),
                    ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("DeepSeek API request failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildRequestBody(AgentModelRequest request) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("model", request.model().modelKey());
        payload.put("messages", request.messages().stream().map(this::toMessagePayload).toList());
        if (!request.tools().isEmpty()) {
            payload.put("tools", request.tools().stream().map(this::toToolPayload).toList());
            payload.put("tool_choice", "auto");
        }
        return payload;
    }

    private Map<String, Object> toMessagePayload(AgentModelMessage message) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("role", message.role().name().toLowerCase(java.util.Locale.ROOT));
        switch (message.role()) {
            case SYSTEM, USER -> payload.put("content", message.content());
            case ASSISTANT -> {
                payload.put("content", message.content());
                if (message.toolCalls() != null) {
                    payload.put(
                            "tool_calls",
                            message.toolCalls().stream().map(this::toAssistantToolCallPayload).toList());
                }
            }
            case TOOL -> {
                payload.put("content", message.content());
                payload.put("tool_call_id", message.toolCallId());
            }
        }
        return payload;
    }

    private Map<String, Object> toAssistantToolCallPayload(AgentToolCall toolCall) {
        return Map.of(
                "id",
                toolCall.toolCallId(),
                "type",
                "function",
                "function",
                Map.of("name", toolCall.toolName(), "arguments", toolCall.argumentsJson()));
    }

    private Map<String, Object> toToolPayload(ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolDefinition tool) {
        return Map.of(
                "type",
                "function",
                "function",
                Map.of(
                        "name",
                        tool.name(),
                        "description",
                        tool.description(),
                        "parameters",
                        parseJson(tool.inputSchemaJson())));
    }

    private JsonNode parseJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse JSON payload", ex);
        }
    }

    private String normalizeFinalOutput(String content) {
        var trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            var newlineIndex = trimmed.indexOf('\n');
            if (newlineIndex > 0) {
                trimmed = trimmed.substring(newlineIndex + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String responseBody(RestClientResponseException ex) {
        var body = ex.getResponseBodyAsString();
        return (body == null || body.isBlank()) ? "<empty body>" : body;
    }

    private String defaultArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return "{}";
        }
        return arguments;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize DeepSeek payload", ex);
        }
    }

    private static String safeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey;
    }

    private boolean isApiKeyMissing() {
        return properties.apiKey() == null || properties.apiKey().isBlank();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " must not be blank");
        }
        return value;
    }

    private record DeepSeekChatCompletionResponse(List<DeepSeekChoice> choices) {}

    private record DeepSeekChoice(DeepSeekMessage message, @JsonProperty("finish_reason") String finishReason) {}

    private record DeepSeekMessage(String role, String content, @JsonProperty("tool_calls") List<DeepSeekToolCall> toolCalls) {}

    private record DeepSeekToolCall(String id, String type, DeepSeekFunction function) {}

    private record DeepSeekFunction(String name, String arguments) {}
}
