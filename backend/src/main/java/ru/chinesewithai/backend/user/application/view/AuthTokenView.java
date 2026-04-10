package ru.chinesewithai.backend.user.application.view;

public record AuthTokenView(String accessToken, String tokenType, long expiresInSeconds) {}
