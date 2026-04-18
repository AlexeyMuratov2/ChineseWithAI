package ru.chinesewithai.backend.agentruntime.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record PreGenerationState(List<PreGenerationContextSection> contextSections, Map<String, JsonNode> artifacts) {

    public static final PreGenerationState EMPTY = new PreGenerationState(List.of(), Map.of());

    public PreGenerationState {
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

    public static PreGenerationState empty() {
        return EMPTY;
    }

    public Optional<JsonNode> findArtifact(String artifactKey) {
        if (artifactKey == null || artifactKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(artifacts.get(artifactKey));
    }
}
