package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStep;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepResult;

@Component
public class InputJsonFieldPreGenerationStep implements PreGenerationStep {

    private final ObjectMapper objectMapper;

    public InputJsonFieldPreGenerationStep(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "input-json-field";
    }

    @Override
    public PreGenerationStepResult execute(PreGenerationStepRequest request) {
        var field = requireText(request.params().path("field").asText(null), "field");
        var target = PreGenerationContextSectionTarget.fromValue(request.params().path("target").asText(null));
        var title = normalizeOptional(request.params().path("title").asText(null));
        var artifactKey = normalizeOptional(request.params().path("artifactKey").asText(null));

        if (request.session().inputJson() == null) {
            throw new IllegalArgumentException("Session input JSON is empty");
        }

        var input = readJson(request.session().inputJson());
        if (!input.isObject()) {
            throw new IllegalArgumentException("Session input JSON must be an object");
        }
        var value = input.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Input JSON field not found: " + field);
        }

        var section = new PreGenerationContextSection(
                target,
                title == null ? "Input field: " + field : title,
                field + ": " + renderValue(value));
        var resolvedArtifactKey = artifactKey == null ? "inputField." + field : artifactKey;
        return new PreGenerationStepResult(List.of(section), Map.of(resolvedArtifactKey, value));
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse session input JSON", ex);
        }
    }

    private String renderValue(JsonNode value) {
        if (value.isValueNode()) {
            return value.asText();
        }
        return value.toString();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
