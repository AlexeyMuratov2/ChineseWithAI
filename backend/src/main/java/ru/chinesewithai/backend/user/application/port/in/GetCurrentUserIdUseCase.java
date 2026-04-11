package ru.chinesewithai.backend.user.application.port.in;

import java.util.UUID;

public interface GetCurrentUserIdUseCase {
    UUID getCurrentUserId();
}
