package ru.chinesewithai.backend.storedfile.application.command;

import java.util.Optional;
import ru.chinesewithai.backend.storedfile.application.security.UploadScenario;

/**
 * Parameters for storing a blob outside an HTTP upload session (other modules). Same security
 * scenario selection as HTTP uploads.
 */
public record StoreFileCommand(
        UploadScenario scenario,
        Optional<String> contentType,
        Optional<String> originalFileName,
        long contentLength) {

    public StoreFileCommand {
        if (contentLength <= 0) {
            throw new IllegalArgumentException("contentLength must be positive");
        }
    }
}
