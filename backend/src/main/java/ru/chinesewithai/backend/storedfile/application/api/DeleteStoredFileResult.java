package ru.chinesewithai.backend.storedfile.application.api;

/**
 * Outcome of a delete attempt. Callers can branch on {@link #ALREADY_ABSENT} for idempotent
 * orchestration without treating it as an error.
 */
public enum DeleteStoredFileResult {
    SUCCESS,
    ALREADY_ABSENT,
    STORAGE_FAILURE
}
