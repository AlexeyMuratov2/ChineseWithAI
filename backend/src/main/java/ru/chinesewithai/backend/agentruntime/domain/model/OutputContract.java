package ru.chinesewithai.backend.agentruntime.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record OutputContract(Map<String, OutputFieldType> requiredFields, String rawJson) {

    public OutputContract {
        Objects.requireNonNull(requiredFields, "requiredFields must not be null");
        requiredFields = Collections.unmodifiableMap(new LinkedHashMap<>(requiredFields));
        rawJson = requireText(rawJson, "rawJson");
    }

    public static OutputContract ofRequiredFields(Map<String, OutputFieldType> requiredFields) {
        var normalizedRequiredFields = Collections.unmodifiableMap(new LinkedHashMap<>(requiredFields));
        return new OutputContract(normalizedRequiredFields, toRawJson(normalizedRequiredFields));
    }

    public boolean hasRequiredField(String fieldName, OutputFieldType type) {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return type.equals(requiredFields.get(fieldName));
    }

    public boolean hasAllRequiredFields(Map<String, OutputFieldType> expectedFields) {
        Objects.requireNonNull(expectedFields, "expectedFields must not be null");
        for (var entry : expectedFields.entrySet()) {
            if (!hasRequiredField(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesRequiredFieldsExactly(Map<String, OutputFieldType> expectedFields) {
        Objects.requireNonNull(expectedFields, "expectedFields must not be null");
        return requiredFields.equals(expectedFields);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        var normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String toRawJson(Map<String, OutputFieldType> requiredFields) {
        var builder = new StringBuilder("{\"requiredFields\":{");
        boolean first = true;
        for (var entry : requiredFields.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"')
                    .append(escapeJson(entry.getKey()))
                    .append("\":\"")
                    .append(entry.getValue().name().toLowerCase(Locale.ROOT))
                    .append('"');
            first = false;
        }
        builder.append("}}");
        return builder.toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
