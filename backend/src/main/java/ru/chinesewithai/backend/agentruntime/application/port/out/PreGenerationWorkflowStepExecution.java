package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PreGenerationWorkflowStepExecution(
        String stepKey,
        JsonNode params,
        List<PreGenerationContextSection> emittedContextSections,
        Map<String, JsonNode> emittedArtifacts) {

    public PreGenerationWorkflowStepExecution {
        stepKey = requireText(stepKey, "stepKey");
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(emittedContextSections, "emittedContextSections must not be null");
        Objects.requireNonNull(emittedArtifacts, "emittedArtifacts must not be null");
        emittedContextSections = List.copyOf(emittedContextSections);
        var copiedArtifacts = new LinkedHashMap<String, JsonNode>();
        emittedArtifacts.forEach((key, value) -> copiedArtifacts.put(
                requireText(key, "artifactKey"), Objects.requireNonNull(value, "artifact value must not be null")));
        emittedArtifacts = Map.copyOf(copiedArtifacts);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
