package ru.chinesewithai.backend.user.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.chinesewithai.backend.user.api.dto.CurrentUserResponse;
import ru.chinesewithai.backend.user.application.port.in.GetCurrentUserUseCase;

@RestController
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
