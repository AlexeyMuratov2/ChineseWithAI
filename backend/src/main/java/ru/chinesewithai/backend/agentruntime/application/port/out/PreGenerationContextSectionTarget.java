package ru.chinesewithai.backend.agentruntime.application.port.out;

public enum PreGenerationContextSectionTarget {
    SYSTEM,
    USER;

    public static PreGenerationContextSectionTarget fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pre-generation context section target must not be blank");
        }
        return valueOf(value.trim().toUpperCase());
    }
}
