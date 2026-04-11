package ru.chinesewithai.backend.storedfile.application.exception;

/** Thrown when an active {@link ru.chinesewithai.backend.storedfile.application.security.FileUploadSecurityStrategy} rejects the upload. */
public class FileUploadRejectedException extends RuntimeException {

    public FileUploadRejectedException(String message) {
        super(message);
    }
}
