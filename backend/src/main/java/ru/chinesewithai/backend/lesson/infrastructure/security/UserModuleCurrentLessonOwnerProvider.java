package ru.chinesewithai.backend.lesson.infrastructure.security;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.port.out.CurrentLessonOwnerProvider;
import ru.chinesewithai.backend.user.application.port.in.GetCurrentUserIdUseCase;

@Component
public class UserModuleCurrentLessonOwnerProvider implements CurrentLessonOwnerProvider {

    private final GetCurrentUserIdUseCase getCurrentUserIdUseCase;

    public UserModuleCurrentLessonOwnerProvider(GetCurrentUserIdUseCase getCurrentUserIdUseCase) {
        this.getCurrentUserIdUseCase = getCurrentUserIdUseCase;
    }

    @Override
    public UUID getCurrentOwnerId() {
        return getCurrentUserIdUseCase.getCurrentUserId();
    }
}
