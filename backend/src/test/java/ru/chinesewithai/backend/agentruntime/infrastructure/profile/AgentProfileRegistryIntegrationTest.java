package ru.chinesewithai.backend.agentruntime.infrastructure.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.chinesewithai.backend.AbstractIntegrationTest;
import ru.chinesewithai.backend.TestcontainersConfiguration;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentProfileRegistry;
import ru.chinesewithai.backend.agentruntime.domain.model.OutputFieldType;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AgentProfileRegistryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AgentProfileRegistry agentProfileRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanRuntimeData() {
        jdbcTemplate.update("DELETE FROM agent_steps");
        jdbcTemplate.update("DELETE FROM agent_sessions");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void seededProfilesLoadWithVisibilityAndParsedPolicies() {
        var hiddenProfile = agentProfileRegistry.findByProfileKey("test-agent:v1");
        var lessonGeneratorProfile = agentProfileRegistry.findByProfileKey("lesson-generator:v1");
        var hsk5LessonGeneratorProfile = agentProfileRegistry.findByProfileKey("lesson-generator:hsk5_v1");
        var grammarExerciseGeneratorProfile = agentProfileRegistry.findByProfileKey("grammar-exercise-generator:v1");
        var visibleProfiles = agentProfileRegistry.findVisibleProfiles();

        assertThat(hiddenProfile).isPresent();
        assertThat(hiddenProfile.orElseThrow().visible()).isFalse();
        assertThat(hiddenProfile.orElseThrow().contextBuilderKey()).isEqualTo("default");
        assertThat(hiddenProfile.orElseThrow().allowedToolNames()).containsExactly("get_static_test_data");
        assertThat(hiddenProfile.orElseThrow().executionPolicy().maxSteps()).isEqualTo(4);
        assertThat(hiddenProfile.orElseThrow().memoryPolicy().includePreviousSteps()).isTrue();
        assertThat(hiddenProfile.orElseThrow().outputContract().requiredFields())
                .containsEntry("summary", OutputFieldType.STRING)
                .containsEntry("toolMessage", OutputFieldType.STRING);

        assertThat(lessonGeneratorProfile).isPresent();
        assertThat(lessonGeneratorProfile.orElseThrow().visible()).isFalse();
        assertThat(lessonGeneratorProfile.orElseThrow().allowedToolNames()).isEmpty();
        assertThat(lessonGeneratorProfile.orElseThrow().autoRepairInvalidOutputEnabled()).isTrue();
        assertThat(lessonGeneratorProfile.orElseThrow().outputContract().requiredFields())
                .containsEntry("schemaVersion", OutputFieldType.NUMBER)
                .containsEntry("moduleKey", OutputFieldType.STRING)
                .containsEntry("newWords", OutputFieldType.ARRAY)
                .containsEntry("sections", OutputFieldType.ARRAY);
        assertThat(lessonGeneratorProfile.orElseThrow().outputContract().rawJson())
                .contains("\"schemaVersion\":\"number\"");

        assertThat(hsk5LessonGeneratorProfile).isPresent();
        assertThat(hsk5LessonGeneratorProfile.orElseThrow().visible()).isFalse();
        assertThat(hsk5LessonGeneratorProfile.orElseThrow().autoRepairInvalidOutputEnabled()).isTrue();
        assertThat(hsk5LessonGeneratorProfile.orElseThrow().systemPrompt())
                .contains("sentences")
                .contains("exampleSentences")
                .contains("answerWord")
                .contains("expectedWord");
        assertThat(hsk5LessonGeneratorProfile.orElseThrow().outputContract().requiredFields())
                .containsEntry("schemaVersion", OutputFieldType.NUMBER)
                .containsEntry("moduleKey", OutputFieldType.STRING)
                .containsEntry("newWords", OutputFieldType.ARRAY)
                .containsEntry("reviewWords", OutputFieldType.ARRAY)
                .containsEntry("sections", OutputFieldType.ARRAY);

        assertThat(grammarExerciseGeneratorProfile).isPresent();
        assertThat(grammarExerciseGeneratorProfile.orElseThrow().visible()).isFalse();
        assertThat(grammarExerciseGeneratorProfile.orElseThrow().allowedToolNames()).isEmpty();
        assertThat(grammarExerciseGeneratorProfile.orElseThrow().autoRepairInvalidOutputEnabled()).isTrue();
        assertThat(grammarExerciseGeneratorProfile.orElseThrow().outputContract().requiredFields())
                .containsEntry("schemaVersion", OutputFieldType.NUMBER)
                .containsEntry("explanationLanguage", OutputFieldType.STRING)
                .containsEntry("explanations", OutputFieldType.ARRAY)
                .containsEntry("usageScenarios", OutputFieldType.ARRAY)
                .containsEntry("exercises", OutputFieldType.ARRAY);

        assertThat(visibleProfiles).extracting(profile -> profile.profileKey()).containsExactly("assistant:v1");
        assertThat(visibleProfiles.getFirst().visible()).isTrue();
        assertThat(visibleProfiles.getFirst().allowedToolNames()).isEmpty();
        assertThat(visibleProfiles.getFirst().autoRepairInvalidOutputEnabled()).isFalse();
        assertThat(visibleProfiles.getFirst().outputContract().requiredFields())
                .containsEntry("answer", OutputFieldType.STRING);
    }
}
