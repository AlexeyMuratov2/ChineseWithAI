package ru.chinesewithai.backend.lesson.infrastructure.pregeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationContextSectionTarget;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationState;
import ru.chinesewithai.backend.agentruntime.application.port.out.PreGenerationStepRequest;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;
import ru.chinesewithai.backend.agentruntime.domain.model.ExecutionPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.MemoryPolicy;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputContract;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;
import ru.chinesewithai.backend.lesson.application.service.VocabularyReviewPlanSelector;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.SuggestedReviewMode;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewPlan;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewPlanItem;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewPolicy;
import ru.chinesewithai.backend.lesson.domain.model.VocabularyReviewReason;

class LessonVocabularyReviewPlanPreGenerationStepTest {

    @Test
    void readsDraftTranslationLanguageAndEmitsArtifactAndSystemSection() {
        var selector = mock(VocabularyReviewPlanSelector.class);
        var objectMapper = new ObjectMapper();
        var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        var plan = new VocabularyReviewPlan(
                List.of(new VocabularyReviewPlanItem(
                        "认识",
                        "rènshi",
                        "to know",
                        0.3d,
                        null,
                        VocabularyReviewReason.RECENTLY_LEARNED,
                        SuggestedReviewMode.RECOGNITION)),
                List.of(),
                VocabularyReviewPolicy.DEFAULT);
        when(selector.select(eq(ownerId), eq(LanguageTag.of("en")), any(Instant.class))).thenReturn(plan);

        var step = new LessonVocabularyReviewPlanPreGenerationStep(selector, objectMapper);
        var result = step.execute(new PreGenerationStepRequest(
                profile(),
                AgentSession.createNew(
                        ownerId,
                        "lesson-generator:v1",
                        "fake-model",
                        "Generate lesson",
                        "{\"draft\":{\"translationLanguage\":\"en\"}}",
                        Instant.now()),
                JsonNodeFactory.instance.objectNode(),
                PreGenerationState.empty()));

        verify(selector).select(eq(ownerId), eq(LanguageTag.of("en")), any(Instant.class));
        assertThat(result.artifacts()).containsKey("vocabularyReviewPlan");
        assertThat(result.contextSections()).singleElement().satisfies(section -> {
            assertThat(section.target()).isEqualTo(PreGenerationContextSectionTarget.SYSTEM);
            assertThat(section.title()).isEqualTo("Vocabulary review plan");
            assertThat(section.content()).contains("\"mustReview\"");
            assertThat(section.content()).contains("认识");
        });
        assertThat(result.artifacts().get("vocabularyReviewPlan").path("mustReview")).hasSize(1);
    }

    private AgentProfile profile() {
        return new AgentProfile(
                "lesson-generator:v1",
                "Lesson Generator v1",
                "Return lesson JSON.",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                new OutputContract(Map.of("title", OutputFieldType.STRING)),
                false,
                null,
                false);
    }
}
