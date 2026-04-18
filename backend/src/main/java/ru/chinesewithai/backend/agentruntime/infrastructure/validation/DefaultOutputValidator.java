package ru.chinesewithai.backend.agentruntime.infrastructure.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidator;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

@Component
public class DefaultOutputValidator implements OutputValidator {

    private static final String VALIDATOR_KEY = "output-contract";

    @Override
    public List<OutputValidationIssue> validate(JsonNode output, OutputContract contract) {
        var issues = new ArrayList<OutputValidationIssue>();

        for (var entry : contract.requiredFields().entrySet()) {
            var fieldName = entry.getKey();
            var value = output.get(fieldName);
            if (value == null || value.isNull()) {
                issues.add(new OutputValidationIssue(
                        VALIDATOR_KEY,
                        "missing_field",
                        fieldName,
                        entry.getValue().name().toLowerCase(Locale.ROOT),
                        "missing",
                        "Missing required output field: " + fieldName));
                continue;
            }
            var issue = validateType(fieldName, value, entry.getValue());
            if (issue != null) {
                issues.add(issue);
            }
        }

        return List.copyOf(issues);
    }

    private OutputValidationIssue validateType(String fieldName, JsonNode value, OutputFieldType type) {
        boolean valid = switch (type) {
            case STRING -> value.isTextual();
            case NUMBER -> value.isNumber();
            case BOOLEAN -> value.isBoolean();
            case OBJECT -> value.isObject();
            case ARRAY -> value.isArray();
        };

        if (valid) {
            return null;
        }
        return new OutputValidationIssue(
                VALIDATOR_KEY,
                "invalid_type",
                fieldName,
                type.name().toLowerCase(Locale.ROOT),
                describeNodeType(value),
                "Output field has invalid type: " + fieldName);
    }

    private String describeNodeType(JsonNode value) {
        if (value == null) {
            return "missing";
        }
        if (value.isTextual()) {
            return "string";
        }
        if (value.isNumber()) {
            return "number";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isObject()) {
            return "object";
        }
        if (value.isArray()) {
            return "array";
        }
        if (value.isNull()) {
            return "null";
        }
        return value.getNodeType().name().toLowerCase(Locale.ROOT);
    }
}
