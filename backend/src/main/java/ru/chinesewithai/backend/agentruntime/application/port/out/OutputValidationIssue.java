package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.Objects;

public record OutputValidationIssue(
        String validator, String code, String path, String expected, String actual, String message) {

    public OutputValidationIssue {
        validator = requireText(validator, "validator");
        code = requireText(code, "code");
        path = normalizePath(path);
        expected = normalizeOptional(expected);
        actual = normalizeOptional(actual);
        message = requireText(message, "message");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        var normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizePath(String value) {
        var normalized = normalizeOptional(value);
        return normalized == null ? "$" : normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
