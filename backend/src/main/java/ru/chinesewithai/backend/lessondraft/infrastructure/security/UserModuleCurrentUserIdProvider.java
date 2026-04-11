package ru.chinesewithai.backend.lessondraft.infrastructure.security;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lessondraft.application.port.out.CurrentUserIdProvider;
import ru.chinesewithai.backend.user.application.port.in.GetCurrentUserIdUseCase;

@Component
public class UserModuleCurrentUserIdProvider implements CurrentUserIdProvider {

    private final GetCurrentUserIdUseCase getCurrentUserIdUseCase;

    public UserModuleCurrentUserIdProvider(GetCurrentUserIdUseCase getCurrentUserIdUseCase) {
        this.getCurrentUserIdUseCase = getCurrentUserIdUseCase;
    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUserIdUseCase.getCurrentUserId();
    }
}
