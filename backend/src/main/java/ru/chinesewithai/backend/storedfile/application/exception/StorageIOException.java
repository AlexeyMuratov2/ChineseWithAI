package ru.chinesewithai.backend.storedfile.application.exception;

/** Object storage (S3/MinIO) failed in a way that should surface to the caller. */
public class StorageIOException extends RuntimeException {

    public StorageIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
