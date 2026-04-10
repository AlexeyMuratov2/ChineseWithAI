package ru.chinesewithai.backend.user.application.port.out;

import ru.chinesewithai.backend.user.domain.model.UserId;

public interface CurrentUserProvider {
    UserId getCurrentUserId();
}
