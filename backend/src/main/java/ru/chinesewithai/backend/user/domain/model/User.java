package ru.chinesewithai.backend.user.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class User {

    private final UserId id;
    private final Username username;
    private final String passwordHash;
    private final String displayName;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(
            UserId id,
            Username username,
            String passwordHash,
            String displayName,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.passwordHash = requireNotBlank(passwordHash, "passwordHash");
        this.displayName = validateDisplayName(displayName);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static User registerNew(Username username, String passwordHash, String displayName, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new User(
                UserId.newId(),
                username,
                passwordHash,
                resolveDisplayName(displayName, username),
                UserStatus.ACTIVE,
                now,
                now);
    }

    public static User reconstitute(
            UserId id,
            Username username,
            String passwordHash,
            String displayName,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new User(id, username, passwordHash, displayName, status, createdAt, updatedAt);
    }

    public User disable(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new User(id, username, passwordHash, displayName, UserStatus.DISABLED, createdAt, now);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UserId id() {
        return id;
    }

    public Username username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String displayName() {
        return displayName;
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String requireNotBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String resolveDisplayName(String displayName, Username username) {
        if (displayName == null || displayName.isBlank()) {
            return username.value();
        }
        return validateDisplayName(displayName);
    }

    private static String validateDisplayName(String displayName) {
        var normalized = requireNotBlank(displayName, "displayName").trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("displayName must be at most 100 chars");
        }
        return normalized;
    }
}
