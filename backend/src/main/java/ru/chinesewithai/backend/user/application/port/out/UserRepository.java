package ru.chinesewithai.backend.user.application.port.out;

import java.util.Optional;
import ru.chinesewithai.backend.user.domain.model.User;
import ru.chinesewithai.backend.user.domain.model.UserId;
import ru.chinesewithai.backend.user.domain.model.Username;

public interface UserRepository {
    boolean existsByUsername(Username username);

    Optional<User> findByUsername(Username username);

    Optional<User> findById(UserId userId);

    User save(User user);
}
