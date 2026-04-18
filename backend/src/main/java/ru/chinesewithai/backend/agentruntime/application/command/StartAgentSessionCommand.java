package ru.chinesewithai.backend.agentruntime.application.command;

import java.util.Objects;

public record StartAgentSessionCommand(
        String profileKey, String modelKey, String task, String inputJson, String systemPromptAppendix) {

    public StartAgentSessionCommand {
        Objects.requireNonNull(profileKey, "profileKey must not be null");
        Objects.requireNonNull(modelKey, "modelKey must not be null");
        Objects.requireNonNull(task, "task must not be null");
    }

    public StartAgentSessionCommand(String profileKey, String modelKey, String task, String inputJson) {
        this(profileKey, modelKey, task, inputJson, null);
    }
}
