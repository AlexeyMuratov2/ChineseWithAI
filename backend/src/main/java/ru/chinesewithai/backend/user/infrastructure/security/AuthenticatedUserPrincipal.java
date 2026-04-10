package ru.chinesewithai.backend.user.infrastructure.security;

import ru.chinesewithai.backend.user.domain.model.UserId;

public record AuthenticatedUserPrincipal(UserId userId, String username, String displayName) {}
