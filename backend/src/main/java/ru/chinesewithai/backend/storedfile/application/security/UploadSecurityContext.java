package ru.chinesewithai.backend.storedfile.application.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Inputs available to security strategies before/after the request body is consumed. Kept free of
 * domain types from other modules so strategies stay reusable.
 */
public record UploadSecurityContext(
        UploadScenario scenario,
        Optional<String> originalFileName,
        Optional<String> declaredContentType,
        Optional<Long> expectedContentLength,
        Optional<UUID> principalId) {

    public static UploadSecurityContext httpUpload(
            UploadScenario scenario,
            String originalFileName,
            String declaredContentType,
            Long expectedContentLength,
            UUID principalId) {
        return new UploadSecurityContext(
                scenario,
                Optional.ofNullable(originalFileName),
                Optional.ofNullable(declaredContentType),
                Optional.ofNullable(expectedContentLength),
                Optional.ofNullable(principalId));
    }

    public static UploadSecurityContext programmatic(UploadScenario scenario) {
        return new UploadSecurityContext(
                scenario, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
