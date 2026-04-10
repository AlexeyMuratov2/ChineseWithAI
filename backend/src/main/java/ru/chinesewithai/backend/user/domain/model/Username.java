package ru.chinesewithai.backend.user.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Username(String value) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,50}$");

    public Username {
        Objects.requireNonNull(value, "value must not be null");
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-50 chars and contain only lowercase letters, digits, '.', '_' or '-'");
        }
    }

    public static Username of(String rawValue) {
        Objects.requireNonNull(rawValue, "rawValue must not be null");
        var normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return new Username(normalized);
    }
}
