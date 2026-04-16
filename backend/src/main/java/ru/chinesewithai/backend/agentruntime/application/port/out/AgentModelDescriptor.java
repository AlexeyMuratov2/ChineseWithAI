package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;

public record AgentModelDescriptor(String modelKey, String displayName, String providerKey, boolean visible) {

    public AgentModelDescriptor {
        Objects.requireNonNull(modelKey, "modelKey must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(providerKey, "providerKey must not be null");
        if (modelKey.isBlank()) {
            throw new IllegalArgumentException("modelKey must not be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (providerKey.isBlank()) {
            throw new IllegalArgumentException("providerKey must not be blank");
        }
    }
}
