package ru.chinesewithai.backend.agentruntime.infrastructure.security;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.CurrentAgentOwnerProvider;
import ru.chinesewithai.backend.user.application.port.in.GetCurrentUserIdUseCase;

@Component
public class UserModuleCurrentAgentOwnerProvider implements CurrentAgentOwnerProvider {

    private final GetCurrentUserIdUseCase getCurrentUserIdUseCase;

    public UserModuleCurrentAgentOwnerProvider(GetCurrentUserIdUseCase getCurrentUserIdUseCase) {
        this.getCurrentUserIdUseCase = getCurrentUserIdUseCase;
    }

    @Override
    public UUID getCurrentOwnerId() {
        return getCurrentUserIdUseCase.getCurrentUserId();
    }
}
