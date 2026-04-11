package ru.chinesewithai.backend.lessondraft.application.port.out;

import java.util.UUID;

public interface CurrentUserIdProvider {
    UUID getCurrentUserId();
}
