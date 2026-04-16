package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;

public record AgentToolDefinition(String name, String description, String inputSchemaJson) {

    public AgentToolDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(inputSchemaJson, "inputSchemaJson must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (inputSchemaJson.isBlank()) {
            throw new IllegalArgumentException("inputSchemaJson must not be blank");
        }
    }
}
