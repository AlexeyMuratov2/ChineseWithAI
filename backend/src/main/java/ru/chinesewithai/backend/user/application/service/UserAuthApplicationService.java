package ru.chinesewithai.backend.user.application.service;

import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.user.application.command.LoginCommand;
import ru.chinesewithai.backend.user.application.command.RegisterUserCommand;
import ru.chinesewithai.backend.user.application.exception.AccountDisabledException;
import ru.chinesewithai.backend.user.application.exception.DuplicateUsernameException;
import ru.chinesewithai.backend.user.application.exception.InvalidCredentialsException;
import ru.chinesewithai.backend.user.application.port.in.LoginUserUseCase;
import ru.chinesewithai.backend.user.application.port.in.RegisterUserUseCase;
import ru.chinesewithai.backend.user.application.port.out.JwtTokenIssuer;
import ru.chinesewithai.backend.user.application.port.out.PasswordHasher;
import ru.chinesewithai.backend.user.application.port.out.UserRepository;
import ru.chinesewithai.backend.user.application.view.AuthTokenView;
import ru.chinesewithai.backend.user.application.view.UserProfileView;
import ru.chinesewithai.backend.user.domain.model.User;
import ru.chinesewithai.backend.user.domain.model.Username;

@Service
public class UserAuthApplicationService implements RegisterUserUseCase, LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenIssuer jwtTokenIssuer;

    public UserAuthApplicationService(
            UserRepository userRepository, PasswordHasher passwordHasher, JwtTokenIssuer jwtTokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtTokenIssuer = jwtTokenIssuer;
    }

    @Override
    @Transactional
    public UserProfileView register(RegisterUserCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var username = Username.of(command.username());
        requirePassword(command.password());

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username.value());
        }

        var passwordHash = passwordHasher.hash(command.password());
        var user = User.registerNew(username, passwordHash, command.displayName(), Instant.now());

        var saved = userRepository.save(user);
        return toProfileView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenView login(LoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var username = Username.of(command.username());
        requirePassword(command.password());
        var user = userRepository.findByUsername(username).orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        return jwtTokenIssuer.issue(user);
    }

    static UserProfileView toProfileView(User user) {
        return new UserProfileView(
                user.id().value(),
                user.username().value(),
                user.displayName(),
                user.status(),
                user.createdAt(),
                user.updatedAt());
    }

    private static void requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
    }
}
