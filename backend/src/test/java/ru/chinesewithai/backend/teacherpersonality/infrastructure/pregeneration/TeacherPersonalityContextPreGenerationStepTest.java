package ru.chinesewithai.backend.teacherpersonality.infrastructure.pregeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import ru.chinesewithai.backend.teacherpersonality.infrastructure.persistence.SpringDataTeacherPersonalityContextJpaRepository;
import ru.chinesewithai.backend.teacherpersonality.infrastructure.persistence.TeacherPersonalityContextJpaEntity;

class TeacherPersonalityContextPreGenerationStepTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emitsRawTeacherPersonalityAsSystemSectionAndArtifact() throws Exception {
        var repository = mock(SpringDataTeacherPersonalityContextJpaRepository.class);
        var context = mock(TeacherPersonalityContextJpaEntity.class);
        var content = objectMapper.readTree(
                "{\"teacherCharacter\":\"\u0422\u044b \u0432\u0435\u0441\u0435\u043b\u044b\u0439 "
                        + "\u0443\u0447\u0438\u0442\u0435\u043b\u044c\","
                        + "\"arbitrary\":{\"tone\":[\"cheerful\",{\"support\":true}],\"metadata\":5}}");
        when(context.getContentJson()).thenReturn(content);
        when(repository.findByProfileKeyAndActiveTrue("lesson-generator:hsk5_v1")).thenReturn(Optional.of(context));

        var step = new TeacherPersonalityContextPreGenerationStep(repository, objectMapper);
        var result = step.execute(request("lesson-generator:hsk5_v1"));

        verify(repository).findByProfileKeyAndActiveTrue("lesson-generator:hsk5_v1");
        assertThat(result.artifacts()).containsEntry("teacherPersonalityContext", content);
        assertThat(result.contextSections()).singleElement().satisfies(section -> {
            assertThat(section.target()).isEqualTo(PreGenerationContextSectionTarget.SYSTEM);
            assertThat(section.title()).isEqualTo("Teacher personality context");
            assertThat(section.content()).contains("\u0422\u044b \u0432\u0435\u0441\u0435\u043b\u044b\u0439");
            assertThat(section.content()).contains("\"arbitrary\"");
            assertThat(section.content()).contains("\"support\" : true");
            assertThat(section.content()).contains("\n");
        });
    }

    @Test
    void failsFastWhenActiveContextIsMissing() {
        var repository = mock(SpringDataTeacherPersonalityContextJpaRepository.class);
        when(repository.findByProfileKeyAndActiveTrue("lesson-generator:hsk5_v1")).thenReturn(Optional.empty());

        var step = new TeacherPersonalityContextPreGenerationStep(repository, objectMapper);

        assertThatThrownBy(() -> step.execute(request("lesson-generator:hsk5_v1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing active teacher personality context for profile: lesson-generator:hsk5_v1");
    }

    private PreGenerationStepRequest request(String profileKey) {
        return new PreGenerationStepRequest(
                profile(profileKey),
                AgentSession.createNew(
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
