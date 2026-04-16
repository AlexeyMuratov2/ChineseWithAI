package ru.chinesewithai.backend.user.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.chinesewithai.backend.config.OpenApiConfig;
import ru.chinesewithai.backend.user.api.dto.CurrentUserResponse;
import ru.chinesewithai.backend.user.application.port.in.GetCurrentUserUseCase;

@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/users")
public class CurrentUserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public CurrentUserController(GetCurrentUserUseCase getCurrentUserUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        var profile = getCurrentUserUseCase.getCurrentUser();
        return new CurrentUserResponse(
                profile.id(),
                profile.username(),
                profile.displayName(),
                profile.status(),
                profile.createdAt(),
                profile.updatedAt());
    }
}
