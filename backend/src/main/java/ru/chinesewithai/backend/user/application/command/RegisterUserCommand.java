package ru.chinesewithai.backend.user.application.command;

public record RegisterUserCommand(String username, String password, String displayName) {}
