package ru.chinesewithai.backend.lessondraft.application.exception;

public class SourceOrderMismatchException extends RuntimeException {

    public SourceOrderMismatchException(String message) {
        super(message);
    }
}
