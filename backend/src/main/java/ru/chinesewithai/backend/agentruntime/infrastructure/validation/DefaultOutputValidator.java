package ru.chinesewithai.backend.agentruntime.infrastructure.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidator;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

@Component
public class DefaultOutputValidator implements OutputValidator {

    private final ObjectMapper objectMapper;

    public DefaultOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void validate(String outputJson, OutputContract contract) {
        var root = readJson(outputJson);
        if (!root.isObject()) {
            throw new IllegalArgumentException("Final output must be a JSON object");
        }

        for (var entry : contract.requiredFields().entrySet()) {
            var value = root.get(entry.getKey());
            if (value == null || value.isNull()) {
                throw new IllegalArgumentException("Missing required output field: " + entry.getKey());
            }
            validateType(entry.getKey(), value, entry.getValue());
        }
    }

    private void validateType(String fieldName, JsonNode value, OutputFieldType type) {
        boolean valid = switch (type) {
            case STRING -> value.isTextual();
            case NUMBER -> value.isNumber();
            case BOOLEAN -> value.isBoolean();
            case OBJECT -> value.isObject();
            case ARRAY -> value.isArray();
        };

        if (!valid) {
            throw new IllegalArgumentException("Output field has invalid type: " + fieldName);
        }
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Final output must be valid JSON", ex);
        }
    }
}
