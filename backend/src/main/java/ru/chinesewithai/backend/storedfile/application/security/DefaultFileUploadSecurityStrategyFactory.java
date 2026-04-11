package ru.chinesewithai.backend.storedfile.application.security;

import org.springframework.stereotype.Component;

/**
 * Central registry of scenario → strategy. Today every scenario maps to the permissive bean; later,
 * inject a {@code Map<UploadScenario, FileUploadSecurityStrategy>} or individual named beans.
 */
@Component
public class DefaultFileUploadSecurityStrategyFactory implements FileUploadSecurityStrategyFactory {

    private final PermissiveFileUploadSecurityStrategy permissive;

    public DefaultFileUploadSecurityStrategyFactory(PermissiveFileUploadSecurityStrategy permissive) {
        this.permissive = permissive;
    }

    @Override
    public FileUploadSecurityStrategy forScenario(UploadScenario scenario) {
        /*
         * Extension point: switch (scenario) { case AVATAR -> avatarStrategy; default -> permissive; }
         * Strategies should remain stateless beans where possible.
         */
        return switch (scenario) {
            case GENERIC_UPLOAD, CROSS_MODULE_STREAM -> permissive;
        };
    }
}
