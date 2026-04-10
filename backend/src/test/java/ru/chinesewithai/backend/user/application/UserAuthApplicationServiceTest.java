package ru.chinesewithai.backend.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chinesewithai.backend.user.application.command.LoginCommand;
import ru.chinesewithai.backend.user.application.command.RegisterUserCommand;
import ru.chinesewithai.backend.user.application.exception.AccountDisabledException;
import ru.chinesewithai.backend.user.application.exception.DuplicateUsernameException;
import ru.chinesewithai.backend.user.application.exception.InvalidCredentialsException;
import ru.chinesewithai.backend.user.application.port.out.JwtTokenIssuer;
import ru.chinesewithai.backend.user.application.port.out.PasswordHasher;
import ru.chinesewithai.backend.user.application.port.out.UserRepository;
import ru.chinesewithai.backend.user.application.service.UserAuthApplicationService;
import ru.chinesewithai.backend.user.application.view.AuthTokenView;
import ru.chinesewithai.backend.user.domain.model.User;
import ru.chinesewithai.backend.user.domain.model.UserId;
import ru.chinesewithai.backend.user.domain.model.UserStatus;
import ru.chinesewithai.backend.user.domain.model.Username;

@ExtendWith(MockitoExtension.class)
class UserAuthApplicationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final JwtTokenIssuer jwtTokenIssuer = mock(JwtTokenIssuer.class);

    private final UserAuthApplicationService service =
            new UserAuthApplicationService(userRepository, passwordHasher, jwtTokenIssuer);

    @Test
    void registerCreatesUserWithCanonicalUsernameAndDefaultDisplayName() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordHasher.hash("StrongPass123!")).thenReturn("hashed-password");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.register(new RegisterUserCommand("Test_User", "StrongPass123!", null));

        assertThat(result.username()).isEqualTo("test_user");
        assertThat(result.displayName()).isEqualTo("test_user");
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().passwordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerFailsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername(Username.of("existing_user"))).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand("existing_user", "StrongPass123!", null)))
                .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    void loginFailsWithInvalidCredentials() {
        when(userRepository.findByUsername(Username.of("user_one")))
                .thenReturn(Optional.of(activeUser("user_one", "hashed-password")));
        when(passwordHasher.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("user_one", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginFailsWhenAccountDisabled() {
        var disabled = User.reconstitute(
                UserId.newId(),
                Username.of("disabled_user"),
                "hash",
                "Disabled User",
                UserStatus.DISABLED,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(600));
        when(userRepository.findByUsername(Username.of("disabled_user"))).thenReturn(Optional.of(disabled));
        when(passwordHasher.matches("StrongPass123!", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("disabled_user", "StrongPass123!")))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        var user = activeUser("john_doe", "hashed-password");
        when(userRepository.findByUsername(Username.of("john_doe"))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("StrongPass123!", "hashed-password")).thenReturn(true);
        when(jwtTokenIssuer.issue(user)).thenReturn(new AuthTokenView("token", "Bearer", 900));

        var result = service.login(new LoginCommand("john_doe", "StrongPass123!"));

        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresInSeconds()).isEqualTo(900);
    }

    private static User activeUser(String username, String passwordHash) {
        return User.reconstitute(
                UserId.newId(),
                Username.of(username),
                passwordHash,
                "John Doe",
                UserStatus.ACTIVE,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(300));
    }
}
