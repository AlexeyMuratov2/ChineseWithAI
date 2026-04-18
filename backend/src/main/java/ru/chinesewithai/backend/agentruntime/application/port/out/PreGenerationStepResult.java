package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PreGenerationStepResult(List<PreGenerationContextSection> contextSections, Map<String, JsonNode> artifacts) {

    public static final PreGenerationStepResult EMPTY = new PreGenerationStepResult(List.of(), Map.of());

    public PreGenerationStepResult {
        Objects.requireNonNull(contextSections, "contextSections must not be null");
        Objects.requireNonNull(artifacts, "artifacts must not be null");
        contextSections = List.copyOf(contextSections);
        var copiedArtifacts = new LinkedHashMap<String, JsonNode>();
        artifacts.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("artifact key must not be blank");
            }
            copiedArtifacts.put(key, Objects.requireNonNull(value, "artifact value must not be null"));
        });
        artifacts = Map.copyOf(copiedArtifacts);
    }

    public static PreGenerationStepResult empty() {
        return EMPTY;
    }
}
