package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStep;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepResult;
import ru.chinesewithai.backend.user.application.port.in.GetCurrentUserSummaryUseCase;

@Component
public class CurrentUserProfilePreGenerationStep implements PreGenerationStep {

    private static final String ARTIFACT_KEY = "currentUserProfile";

    private final GetCurrentUserSummaryUseCase getCurrentUserSummaryUseCase;

    public CurrentUserProfilePreGenerationStep(GetCurrentUserSummaryUseCase getCurrentUserSummaryUseCase) {
        this.getCurrentUserSummaryUseCase = getCurrentUserSummaryUseCase;
    }

    @Override
    public String key() {
        return "current-user-profile";
    }

    @Override
    public PreGenerationStepResult execute(PreGenerationStepRequest request) {
        var user = getCurrentUserSummaryUseCase.getCurrentUserSummary();
        var artifact = new ObjectNode(JsonNodeFactory.instance);
        artifact.put("id", user.id().toString());
        artifact.put("username", user.username());
        artifact.put("displayName", user.displayName());
        artifact.put("status", user.status());

        var section = new PreGenerationContextSection(
                PreGenerationContextSectionTarget.SYSTEM,
                "Current user profile",
                """
                id: %s
                username: %s
                displayName: %s
                status: %s
                """
                        .formatted(user.id(), user.username(), user.displayName(), user.status())
                        .trim());
        return new PreGenerationStepResult(List.of(section), Map.of(ARTIFACT_KEY, artifact));
    }
}
