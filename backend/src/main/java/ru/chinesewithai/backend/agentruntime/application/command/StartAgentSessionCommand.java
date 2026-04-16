package ru.chinesewithai.backend.agentruntime.application.command;

import java.util.Objects;

public record StartAgentSessionCommand(String profileKey, String modelKey, String task, String inputJson) {

    public StartAgentSessionCommand {
        Objects.requireNonNull(profileKey, "profileKey must not be null");
        Objects.requireNonNull(modelKey, "modelKey must not be null");
        Objects.requireNonNull(task, "task must not be null");
    }
}
