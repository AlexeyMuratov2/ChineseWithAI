package ru.chinesewithai.backend.user.application.port.in;

import java.util.UUID;

public record CurrentUserSummaryView(UUID id, String username, String displayName, String status) {}
