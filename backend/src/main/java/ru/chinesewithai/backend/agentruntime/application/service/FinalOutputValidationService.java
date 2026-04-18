package ru.chinesewithai.backend.agentruntime.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationIssue;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationResult;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategyRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidator;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.infrastructure.validation.OutputValidationStrategyCatalog;

@Component
public class FinalOutputValidationService {

    private static final String BUILT_IN_VALIDATOR = "output-contract";

    private final ObjectMapper objectMapper;
    private final OutputValidator outputValidator;
    private final OutputValidationStrategyCatalog strategyCatalog;

    public FinalOutputValidationService(
            ObjectMapper objectMapper,
            OutputValidator outputValidator,
            OutputValidationStrategyCatalog strategyCatalog) {
        this.objectMapper = objectMapper;
        this.outputValidator = outputValidator;
        this.strategyCatalog = strategyCatalog;
    }

    public OutputValidationResult validate(AgentProfile profile, AgentSession session, String rawOutputJson) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(rawOutputJson);
        } catch (JsonProcessingException ex) {
            return new OutputValidationResult(java.util.List.of(new OutputValidationIssue(
                    BUILT_IN_VALIDATOR,
                    "invalid_json",
                    "$",
                    "valid JSON object",
                    "invalid_json",
                    "Final output must be valid JSON")));
        }

        if (root == null || !root.isObject()) {
            return new OutputValidationResult(java.util.List.of(new OutputValidationIssue(
                    BUILT_IN_VALIDATOR,
                    "root_not_object",
                    "$",
                    "object",
                    describeNodeType(root),
                    "Final output must be a JSON object")));
        }

        var issues = new ArrayList<>(outputValidator.validate(root, profile.outputContract()));
        if (profile.outputValidationStrategyKey() != null) {
            var strategy = strategyCatalog.getRequired(profile.outputValidationStrategyKey());
            issues.addAll(strategy.validate(new OutputValidationStrategyRequest(
                    profile.profileKey(), session.inputJson(), root, rawOutputJson)));
        }
        return new OutputValidationResult(issues);
    }

    private static String describeNodeType(JsonNode node) {
        if (node == null) {
            return "missing";
        }
        if (node.isObject()) {
            return "object";
        }
        if (node.isArray()) {
            return "array";
        }
        if (node.isTextual()) {
            return "string";
        }
        if (node.isBoolean()) {
            return "boolean";
        }
        if (node.isNumber()) {
            return "number";
        }
        if (node.isNull()) {
            return "null";
        }
        return node.getNodeType().name().toLowerCase(java.util.Locale.ROOT);
    }
}
