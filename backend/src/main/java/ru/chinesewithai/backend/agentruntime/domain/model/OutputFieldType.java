package ru.chinesewithai.backend.agentruntime.domain.model;

import java.util.Locale;

public enum OutputFieldType {
    STRING,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY;

    public static OutputFieldType fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Output field type must not be blank");
        }
        return OutputFieldType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
