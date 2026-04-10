package ru.chinesewithai.backend.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import ru.chinesewithai.backend.user.domain.model.UserStatus;

public record CurrentUserResponse(
        UUID id, String username, String displayName, UserStatus status, Instant createdAt, Instant updatedAt) {}
