package ru.chinesewithai.backend.user.application.view;

import java.time.Instant;
import java.util.UUID;
import ru.chinesewithai.backend.user.domain.model.UserStatus;

public record UserProfileView(
        UUID id,
        String username,
        String displayName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {}
