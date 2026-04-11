package ru.chinesewithai.backend.storedfile.application.exception;

import ru.chinesewithai.backend.storedfile.domain.model.UploadSessionState;

public class InvalidUploadSessionStateException extends RuntimeException {

    public InvalidUploadSessionStateException(String message) {
        super(message);
    }

    public static InvalidUploadSessionStateException notReceiving(UploadSessionState actual) {
        return new InvalidUploadSessionStateException("Session is not ready to receive content, state=" + actual);
    }
}
