package ru.chinesewithai.backend.user.application.exception;

public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() {
        super("Authentication is required");
    }
}
