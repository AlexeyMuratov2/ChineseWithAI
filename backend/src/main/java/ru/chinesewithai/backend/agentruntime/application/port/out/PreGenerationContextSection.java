package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;

public record PreGenerationContextSection(
        PreGenerationContextSectionTarget target, String title, String content) {

    public PreGenerationContextSection {
        Objects.requireNonNull(target, "target must not be null");
        title = requireText(title, "title");
        content = requireText(content, "content");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
