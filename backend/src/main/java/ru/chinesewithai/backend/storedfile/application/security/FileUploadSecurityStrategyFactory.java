package ru.chinesewithai.backend.storedfile.application.security;

/**
 * Resolves the active {@link FileUploadSecurityStrategy} for a scenario. Adding a new scenario or
 * stricter strategy should require changes only here (wiring) and a new strategy class — not the
 * upload orchestrator.
 */
public interface FileUploadSecurityStrategyFactory {

    FileUploadSecurityStrategy forScenario(UploadScenario scenario);
}
