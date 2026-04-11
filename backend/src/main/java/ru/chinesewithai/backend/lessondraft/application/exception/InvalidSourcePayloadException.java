package ru.chinesewithai.backend.lessondraft.application.exception;

public class InvalidSourcePayloadException extends RuntimeException {

    public InvalidSourcePayloadException(String message) {
        super(message);
    }
}
