package ru.chinesewithai.backend.lesson.domain.model;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Objects;

public record LanguageTag(String value) {

    public LanguageTag {
        value = normalize(value);
    }

    public static LanguageTag of(String rawValue) {
        return new LanguageTag(rawValue);
    }

    private static String normalize(String rawValue) {
        Objects.requireNonNull(rawValue, "language tag must not be null");
        var candidate = rawValue.trim();
        if (candidate.isBlank()) {
            throw new IllegalArgumentException("language tag must not be blank");
        }

        try {
            var locale = new Locale.Builder().setLanguageTag(candidate).build();
            var normalized = locale.toLanguageTag();
            if ("und".equalsIgnoreCase(normalized)) {
                throw new IllegalArgumentException("language tag must define a concrete language");
            }
            return normalized;
        } catch (IllformedLocaleException ex) {
            throw new IllegalArgumentException("language tag is invalid: " + rawValue, ex);
        }
    }
}
