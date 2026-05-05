package ru.chinesewithai.backend.teacherpersonality.infrastructure.pregeneration;

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
import ru.chinesewithai.backend.teacherpersonality.infrastructure.persistence.SpringDataTeacherPersonalityContextJpaRepository;

@Component
public class TeacherPersonalityContextPreGenerationStep implements PreGenerationStep {

    private static final String STEP_KEY = "teacher-personality-context";
    private static final String ARTIFACT_KEY = "teacherPersonalityContext";
    private static final String SECTION_TITLE = "Teacher personality context";

    private final SpringDataTeacherPersonalityContextJpaRepository repository;
    private final ObjectMapper objectMapper;

    public TeacherPersonalityContextPreGenerationStep(
            SpringDataTeacherPersonalityContextJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return STEP_KEY;
    }

    @Override
    public PreGenerationStepResult execute(PreGenerationStepRequest request) {
        var contextProfileKey = contextProfileKey(request);
        var context = repository
                .findByProfileKeyAndActiveTrue(contextProfileKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing active teacher personality context for profile: " + contextProfileKey));
        var artifact = context.getContentJson();
        var section = new PreGenerationContextSection(
                PreGenerationContextSectionTarget.SYSTEM, SECTION_TITLE, prettyPrint(artifact));
        return new PreGenerationStepResult(List.of(section), Map.of(ARTIFACT_KEY, artifact));
    }

    private String contextProfileKey(PreGenerationStepRequest request) {
        var configured = request.params().path("contextProfileKey").asText(null);
        if (configured == null || configured.isBlank()) {
            return request.profile().profileKey();
        }
        return configured.trim();
    }

    private String prettyPrint(JsonNode artifact) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(artifact);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to render teacher personality context", ex);
        }
    }
}
