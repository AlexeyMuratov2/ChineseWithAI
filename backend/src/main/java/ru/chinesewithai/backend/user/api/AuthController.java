package ru.chinesewithai.backend.user.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.chinesewithai.backend.user.api.dto.LoginRequest;
import ru.chinesewithai.backend.user.api.dto.RegisterRequest;
import ru.chinesewithai.backend.user.api.dto.RegisterResponse;
import ru.chinesewithai.backend.user.api.dto.TokenResponse;
import ru.chinesewithai.backend.user.application.command.LoginCommand;
import ru.chinesewithai.backend.user.application.command.RegisterUserCommand;
import ru.chinesewithai.backend.user.application.port.in.LoginUserUseCase;
import ru.chinesewithai.backend.user.application.port.in.RegisterUserUseCase;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUserUseCase loginUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        var profile = registerUserUseCase.register(
                new RegisterUserCommand(request.username(), request.password(), request.displayName()));

        var response = new RegisterResponse(
                profile.id(),
                profile.username(),
                profile.displayName(),
                profile.status(),
                profile.createdAt(),
                profile.updatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var token = loginUserUseCase.login(new LoginCommand(request.username(), request.password()));
        return new TokenResponse(token.accessToken(), token.tokenType(), token.expiresInSeconds());
    }
}
