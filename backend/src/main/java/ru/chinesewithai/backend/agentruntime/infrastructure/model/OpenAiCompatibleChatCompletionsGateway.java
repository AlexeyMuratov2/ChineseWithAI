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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelDescriptor;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelGateway;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessage;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelResponse;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolCall;

abstract class OpenAiCompatibleChatCompletionsGateway implements AgentModelGateway {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String providerKey;
    private final String providerDisplayName;
    private final String apiKeyConfigurationName;
    private final String apiKey;
    private final AgentModelDescriptor modelDescriptor;
    private final AgentRuntimeProperties agentRuntimeProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    protected OpenAiCompatibleChatCompletionsGateway(
            String providerKey,
            String providerDisplayName,
            String apiKeyConfigurationName,
            AgentModelDescriptor modelDescriptor,
            String baseUrl,
            String apiKey,
            AgentRuntimeProperties agentRuntimeProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        if (!providerKey.equals(modelDescriptor.providerKey())) {
            throw new IllegalArgumentException("Model descriptor provider mismatch: " + modelDescriptor.modelKey());
        }
        this.providerKey = providerKey;
        this.providerDisplayName = providerDisplayName;
        this.apiKeyConfigurationName = apiKeyConfigurationName;
        this.apiKey = apiKey;
        this.modelDescriptor = modelDescriptor;
        this.agentRuntimeProperties = agentRuntimeProperties;
        this.restClient = restClientBuilder
                .clone()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + safeApiKey(apiKey))
                .build();
        this.objectMapper = objectMapper;

        if (isApiKeyMissing()) {
            log.warn("{} model gateway is disabled because {} is blank", providerDisplayName, apiKeyConfigurationName);
        } else {
            log.info("{} model gateway is enabled with base URL {}", providerDisplayName, baseUrl);
        }
    }

    @Override
    public String providerKey() {
        return providerKey;
    }

    @Override
    public List<AgentModelDescriptor> supportedModels() {
        if (isApiKeyMissing()) {
            return List.of();
        }
        return List.of(modelDescriptor);
    }

    @Override
    public AgentModelResponse generate(AgentModelRequest request) {
        if (isApiKeyMissing()) {
            throw new IllegalStateException(providerDisplayName + " API key is not configured");
        }

        var requestBody = buildRequestBody(request);
        if (agentRuntimeProperties.logModelIo()) {
            log.info(
                    "{} model request sessionId={} profileKey={} sessionModelKey={} requestModelKey={} provider={} messageCount={} toolsPresent={} body={}",
                    providerDisplayName,
                    request.session().id(),
                    request.session().profileKey(),
                    request.session().modelKey(),
                    request.model().modelKey(),
                    providerKey(),
                    request.messages().size(),
                    !request.tools().isEmpty(),
                    writeJson(requestBody));
        }

        try {
            var response = restClient
                    .post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(OpenAiChatCompletionResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException(providerDisplayName + " returned no choices");
            }

            var choice = response.choices().getFirst();
            var message = choice.message();
            var rawPayloadJson = writeJson(response);

            if (agentRuntimeProperties.logModelIo()) {
                log.info(
                        "{} model response sessionId={} profileKey={} rawPayload={}",
                        providerDisplayName,
                        request.session().id(),
                        request.session().profileKey(),
                        rawPayloadJson);
            }

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
                throw new IllegalStateException(providerDisplayName + " returned empty final content");
            }

            return AgentModelResponse.finalOutput(rawPayloadJson, normalizeFinalOutput(message.content()));
        } catch (RestClientResponseException ex) {
            if (agentRuntimeProperties.logModelIo()) {
                log.info(
                        "{} model error sessionId={} profileKey={} status={} body={}",
                        providerDisplayName,
                        request.session().id(),
                        request.session().profileKey(),
                        ex.getStatusCode().value(),
                        responseBody(ex));
            }
            throw new IllegalStateException(
                    providerDisplayName
                            + " API request failed with status "
                            + ex.getStatusCode().value()
                            + ": "
                            + responseBody(ex),
                    ex);
        } catch (RestClientException ex) {
            if (agentRuntimeProperties.logModelIo()) {
                log.info(
                        "{} model error sessionId={} profileKey={} message={}",
                        providerDisplayName,
                        request.session().id(),
                        request.session().profileKey(),
                        ex.getMessage());
            }
            throw new IllegalStateException(providerDisplayName + " API request failed: " + ex.getMessage(), ex);
        }
    }

    protected Map<String, Object> additionalRequestParameters(AgentModelRequest request) {
        return Map.of();
    }

    private Map<String, Object> buildRequestBody(AgentModelRequest request) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("model", request.model().modelKey());
        payload.put("messages", request.messages().stream().map(this::toMessagePayload).toList());
        if (!request.tools().isEmpty()) {
            payload.put("tools", request.tools().stream().map(this::toToolPayload).toList());
            payload.put("tool_choice", "auto");
        }
        payload.putAll(additionalRequestParameters(request));
        return payload;
    }

    private Map<String, Object> toMessagePayload(AgentModelMessage message) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("role", message.role().name().toLowerCase(java.util.Locale.ROOT));
        switch (message.role()) {
            case SYSTEM, USER -> payload.put("content", toContentPayload(message));
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

    private Object toContentPayload(AgentModelMessage message) {
        if (message.contentParts() == null) {
            return message.content();
        }
        return message.contentParts().stream().map(this::toContentPartPayload).toList();
    }

    private Map<String, Object> toContentPartPayload(
            ru.chinesewithai.backend.agentruntime.application.port.out.AgentModelMessageContentPart contentPart) {
        return switch (contentPart.type()) {
            case TEXT -> Map.of("type", "text", "text", contentPart.text());
            case IMAGE_URL -> Map.of("type", "image_url", "image_url", Map.of("url", contentPart.imageUrl()));
        };
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

    private Map<String, Object> toToolPayload(
            ru.chinesewithai.backend.agentruntime.application.port.out.AgentToolDefinition tool) {
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
            throw new IllegalStateException("Failed to serialize model payload", ex);
        }
    }

    private static String safeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey;
    }

    private boolean isApiKeyMissing() {
        return apiKey == null || apiKey.isBlank();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " must not be blank");
        }
        return value;
    }

    private record OpenAiChatCompletionResponse(List<OpenAiChoice> choices) {}

    private record OpenAiChoice(OpenAiMessage message, @JsonProperty("finish_reason") String finishReason) {}

    private record OpenAiMessage(String role, String content, @JsonProperty("tool_calls") List<OpenAiToolCall> toolCalls) {}

    private record OpenAiToolCall(String id, String type, OpenAiFunction function) {}

    private record OpenAiFunction(String name, String arguments) {}
}
