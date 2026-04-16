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

        assertThat(visibleProfiles).extracting(profile -> profile.profileKey()).containsExactly("assistant:v1");
        assertThat(visibleProfiles.getFirst().visible()).isTrue();
        assertThat(visibleProfiles.getFirst().allowedToolNames()).isEmpty();
        assertThat(visibleProfiles.getFirst().outputContract().requiredFields())
                .containsEntry("answer", OutputFieldType.STRING);
    }
}
