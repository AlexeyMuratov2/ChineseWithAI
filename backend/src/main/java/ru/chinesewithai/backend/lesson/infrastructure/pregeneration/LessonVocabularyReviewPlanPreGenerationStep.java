package ru.chinesewithai.backend.lesson.infrastructure.pregeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSection;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStep;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepRequest;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepResult;
import ru.chinesewithai.backend.lesson.application.service.VocabularyReviewPlanSelector;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;

@Component
public class LessonVocabularyReviewPlanPreGenerationStep implements PreGenerationStep {

    private static final String STEP_KEY = "lesson-vocabulary-review-plan";
    private static final String ARTIFACT_KEY = "vocabularyReviewPlan";
    private static final String SECTION_TITLE = "Vocabulary review plan";

    private final VocabularyReviewPlanSelector vocabularyReviewPlanSelector;
    private final ObjectMapper objectMapper;

    public LessonVocabularyReviewPlanPreGenerationStep(
            VocabularyReviewPlanSelector vocabularyReviewPlanSelector, ObjectMapper objectMapper) {
        this.vocabularyReviewPlanSelector = vocabularyReviewPlanSelector;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return STEP_KEY;
    }

    @Override
    public PreGenerationStepResult execute(PreGenerationStepRequest request) {
        var translationLanguage = readTranslationLanguage(request.session().inputJson());
        var plan = vocabularyReviewPlanSelector.select(
                request.session().ownerId(), translationLanguage, Instant.now());
        var artifact = objectMapper.valueToTree(plan);
        var section = new PreGenerationContextSection(
                PreGenerationContextSectionTarget.SYSTEM, SECTION_TITLE, prettyPrint(artifact));
        return new PreGenerationStepResult(List.of(section), Map.of(ARTIFACT_KEY, artifact));
    }

    private LanguageTag readTranslationLanguage(String rawInputJson) {
        if (rawInputJson == null || rawInputJson.isBlank()) {
            throw new IllegalArgumentException("Session input JSON is empty");
        }
        try {
            var input = objectMapper.readTree(rawInputJson);
            if (input == null || !input.isObject()) {
                throw new IllegalArgumentException("Session input JSON must be an object");
            }
            var translationLanguage = input.path("draft").path("translationLanguage").asText(null);
            if (translationLanguage == null || translationLanguage.isBlank()) {
                throw new IllegalArgumentException("draft.translationLanguage must be present");
            }
            return LanguageTag.of(translationLanguage);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse session input JSON", ex);
        }
    }

    private String prettyPrint(com.fasterxml.jackson.databind.JsonNode artifact) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(artifact);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to render vocabulary review plan", ex);
        }
    }
}
