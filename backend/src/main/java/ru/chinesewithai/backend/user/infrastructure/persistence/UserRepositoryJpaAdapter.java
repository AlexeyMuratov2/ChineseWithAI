package ru.chinesewithai.backend.user.infrastructure.persistence;

import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.user.application.exception.DuplicateUsernameException;
import ru.chinesewithai.backend.user.application.port.out.UserRepository;
import ru.chinesewithai.backend.user.domain.model.User;
import ru.chinesewithai.backend.user.domain.model.UserId;
import ru.chinesewithai.backend.user.domain.model.Username;

@Repository
public class UserRepositoryJpaAdapter implements UserRepository {

    private final SpringDataUserJpaRepository repository;

    public UserRepositoryJpaAdapter(SpringDataUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByUsername(Username username) {
        return repository.existsByUsername(username.value());
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return repository.findByUsername(username.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return repository.findById(userId.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public User save(User user) {
        try {
            var entity = repository.save(UserJpaMapper.toEntity(user));
            return UserJpaMapper.toDomain(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUsernameException(user.username().value());
        }
    }
}
