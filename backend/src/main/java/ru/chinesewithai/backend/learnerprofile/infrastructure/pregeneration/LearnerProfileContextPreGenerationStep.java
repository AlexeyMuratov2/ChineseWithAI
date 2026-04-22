package ru.chinesewithai.backend.learnerprofile.infrastructure.pregeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStep;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepResult;
import ru.chinesewithai.backend.learnerprofile.infrastructure.persistence.SpringDataLearnerProfileContextJpaRepository;

@Component
public class LearnerProfileContextPreGenerationStep implements PreGenerationStep {

    private static final String STEP_KEY = "learner-profile-context";
    private static final String ARTIFACT_KEY = "learnerProfileContext";
    private static final String SECTION_TITLE = "Learner profile context";

    private final SpringDataLearnerProfileContextJpaRepository repository;
    private final ObjectMapper objectMapper;

    public LearnerProfileContextPreGenerationStep(
            SpringDataLearnerProfileContextJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return STEP_KEY;
    }

    @Override
    public PreGenerationStepResult execute(PreGenerationStepRequest request) {
        var context = repository
                .findByProfileKeyAndActiveTrue(request.profile().profileKey())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing active learner profile context for profile: " + request.profile().profileKey()));
        var artifact = context.getContentJson();
        var section = new PreGenerationContextSection(
                PreGenerationContextSectionTarget.SYSTEM, SECTION_TITLE, prettyPrint(artifact));
        return new PreGenerationStepResult(List.of(section), Map.of(ARTIFACT_KEY, artifact));
    }

    private String prettyPrint(JsonNode artifact) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(artifact);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to render learner profile context", ex);
        }
    }
}
