package ru.chinesewithai.backend.storedfile.application.exception;

import java.util.UUID;

public class UploadSessionNotFoundException extends RuntimeException {

    public UploadSessionNotFoundException(UUID sessionId) {
        super("Upload session not found: " + sessionId);
    }
}
