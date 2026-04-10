package ru.chinesewithai.backend.user.infrastructure.persistence;

import ru.chinesewithai.backend.user.domain.model.User;
import ru.chinesewithai.backend.user.domain.model.UserId;
import ru.chinesewithai.backend.user.domain.model.Username;

final class UserJpaMapper {

    private UserJpaMapper() {}

    static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.id().value(),
                user.username().value(),
                user.passwordHash(),
                user.displayName(),
                user.status(),
                user.createdAt(),
                user.updatedAt());
    }

    static User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                new UserId(entity.getId()),
                Username.of(entity.getUsername()),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
