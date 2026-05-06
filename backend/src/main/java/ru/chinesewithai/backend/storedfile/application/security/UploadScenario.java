package ru.chinesewithai.backend.storedfile.application.security;

/**
 * Selects which {@link FileUploadSecurityStrategy} the factory returns. Callers pass a scenario so
 * future rules can differ (e.g. avatar vs lesson attachment) without changing upload orchestration
 * code.
 */
public enum UploadScenario {
    /**
     * Generic HTTP/API upload initiated by this module's controllers.
     */
    GENERIC_UPLOAD,

    /**
     * User-uploaded material that can be attached to a lesson draft source.
     */
    LESSON_SOURCE,

    /**
     * Reserved for programmatic ingestion from other modules (same permissive policy for now).
     */
    CROSS_MODULE_STREAM
}
