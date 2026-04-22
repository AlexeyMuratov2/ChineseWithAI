package ru.chinesewithai.backend.learnerprofile.infrastructure.pregeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import ru.chinesewithai.backend.learnerprofile.infrastructure.persistence.LearnerProfileContextJpaEntity;
import ru.chinesewithai.backend.learnerprofile.infrastructure.persistence.SpringDataLearnerProfileContextJpaRepository;

class LearnerProfileContextPreGenerationStepTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emitsRawLearnerContextAsSystemSectionAndArtifact() throws Exception {
        var repository = mock(SpringDataLearnerProfileContextJpaRepository.class);
        var context = mock(LearnerProfileContextJpaEntity.class);
        var content = objectMapper.readTree(
                "{\"summary\":\"Ученик уровня HSK5上, хочет изучать разговорные конструкции.\"}");
        when(context.getContentJson()).thenReturn(content);
        when(repository.findByProfileKeyAndActiveTrue("lesson-generator:hsk5_v1")).thenReturn(Optional.of(context));

        var step = new LearnerProfileContextPreGenerationStep(repository, objectMapper);
        var result = step.execute(request("lesson-generator:hsk5_v1"));

        assertThat(result.artifacts()).containsEntry("learnerProfileContext", content);
        assertThat(result.contextSections()).singleElement().satisfies(section -> {
            assertThat(section.target()).isEqualTo(PreGenerationContextSectionTarget.SYSTEM);
            assertThat(section.title()).isEqualTo("Learner profile context");
            assertThat(section.content()).contains("Ученик уровня HSK5上");
            assertThat(section.content()).contains("\n");
        });
    }

    @Test
    void failsFastWhenActiveContextIsMissing() {
        var repository = mock(SpringDataLearnerProfileContextJpaRepository.class);
        when(repository.findByProfileKeyAndActiveTrue("lesson-generator:hsk5_v1")).thenReturn(Optional.empty());

        var step = new LearnerProfileContextPreGenerationStep(repository, objectMapper);

        assertThatThrownBy(() -> step.execute(request("lesson-generator:hsk5_v1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing active learner profile context for profile: lesson-generator:hsk5_v1");
    }

    private PreGenerationStepRequest request(String profileKey) {
        return new PreGenerationStepRequest(
                profile(profileKey),
                AgentSession.createNew(
                        UUID.randomUUID(),
                        profileKey,
                        "fake-model",
                        "Generate lesson",
                        "{\"draft\":{\"translationLanguage\":\"en\"}}",
                        Instant.now()),
                JsonNodeFactory.instance.objectNode(),
                PreGenerationState.empty());
    }

    private AgentProfile profile(String profileKey) {
        return new AgentProfile(
                profileKey,
                "Lesson Generator HSK5 v1",
                "Return lesson JSON.",
                "default",
                List.of(),
                new ExecutionPolicy(4),
                new MemoryPolicy(true, 8),
                OutputContract.ofRequiredFields(Map.of("title", OutputFieldType.STRING)),
                false,
                false);
    }
}
