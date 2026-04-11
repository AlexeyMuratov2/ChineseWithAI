package ru.chinesewithai.backend.storedfile.application.security;

import org.springframework.stereotype.Component;

/**
 * Default no-op policy: accepts every upload. Replace or supplement with real validators (size,
 * MIME sniffing, antivirus hooks) registered in {@link DefaultFileUploadSecurityStrategyFactory}
 * per {@link UploadScenario} without rewriting {@code StoredFileApplicationService}.
 */
@Component
public class PermissiveFileUploadSecurityStrategy implements FileUploadSecurityStrategy {

    @Override
    public void validateBeforeStream(UploadSecurityContext context) {
        // Intentionally empty: permissive baseline for early development.
    }

    @Override
    public void validateAfterStream(UploadSecurityContext context, long actualBytesRead) {
        // Intentionally empty: permissive baseline for early development.
    }
}
