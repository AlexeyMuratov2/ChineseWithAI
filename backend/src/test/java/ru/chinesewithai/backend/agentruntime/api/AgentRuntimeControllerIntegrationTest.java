package ru.chinesewithai.backend.agentruntime.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.chinesewithai.backend.AbstractIntegrationTest;
import ru.chinesewithai.backend.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AgentRuntimeControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanRuntimeData() {
        jdbcTemplate.update("DELETE FROM lesson_generation_run_stages");
        jdbcTemplate.update("DELETE FROM lesson_generation_runs");
        jdbcTemplate.update("DELETE FROM agent_steps");
        jdbcTemplate.update("DELETE FROM agent_sessions");
    }

    @Test
    void startAndGetSessionReturnCompletedTraceAndPersistedState() throws Exception {
        var startPayload = objectMapper.writeValueAsString(new StartPayload(
                "test-agent:v1", "fake-model", "Run a smoke test", Map.of("objective", "smoke-test"), null));

        var startResponse = mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileKey").value("test-agent:v1"))
                .andExpect(jsonPath("$.modelKey").value("fake-model"))
                .andExpect(jsonPath("$.task").value("Run a smoke test"))
                .andExpect(jsonPath("$.input.objective").value("smoke-test"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finalOutput.summary").value("Fake agent completed successfully"))
                .andExpect(jsonPath("$.finalOutput.toolMessage").value("hello-from-static-tool"))
                .andExpect(jsonPath("$.steps.length()").value(13))
                .andExpect(jsonPath("$.steps[0].type").value("SESSION_CREATED"))
                .andExpect(jsonPath("$.steps[1].type").value("PRE_GENERATION_STARTED"))
                .andExpect(jsonPath("$.steps[2].type").value("PRE_GENERATION_COMPLETED"))
                .andExpect(jsonPath("$.steps[12].type").value("SESSION_COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var sessionId = UUID.fromString(objectMapper.readTree(startResponse).get("sessionId").asText());

        mockMvc.perform(get("/api/v1/agent-runtime/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.modelKey").value("fake-model"))
                .andExpect(jsonPath("$.task").value("Run a smoke test"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps.length()").value(13));

        assertCount("SELECT COUNT(*) FROM agent_sessions", 1);
        assertCount("SELECT COUNT(*) FROM agent_steps", 13);
        assertText("SELECT model_key FROM agent_sessions", "fake-model");
        assertText("SELECT task FROM agent_sessions", "Run a smoke test");
    }

    @Test
    void startReturnsNotFoundForUnknownProfile() throws Exception {
        var payload = objectMapper.writeValueAsString(
                new StartPayload("missing-profile", "fake-model", "Do x", Map.of("objective", "x"), null));

        mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void startReturnsNotFoundForUnknownModel() throws Exception {
        var payload = objectMapper.writeValueAsString(
                new StartPayload("test-agent:v1", "missing-model", "Do x", Map.of("objective", "x"), null));

        mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void startWithWorkflowVariantRunsPreGenerationWorkflowAndReturnsVariantInTrace() throws Exception {
        var payload = objectMapper.writeValueAsString(new StartPayload(
                "test-agent:v1",
                "fake-model",
                "Run a personalized smoke test",
                Map.of("objective", "smoke-test", "learnerLevel", "HSK2"),
                "personalized-smoke"));

        var startResponse = mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workflowVariantKey").value("personalized-smoke"))
                .andExpect(jsonPath("$.finalOutput.seenLearnerLevel").value("HSK2"))
                .andExpect(jsonPath("$.steps.length()").value(14))
                .andExpect(jsonPath("$.steps[0].type").value("SESSION_CREATED"))
                .andExpect(jsonPath("$.steps[1].type").value("PRE_GENERATION_STARTED"))
                .andExpect(jsonPath("$.steps[2].type").value("PRE_GENERATION_STEP"))
                .andExpect(jsonPath("$.steps[3].type").value("PRE_GENERATION_COMPLETED"))
                .andExpect(jsonPath("$.steps[4].type").value("CONTEXT_BUILT"))
                .andExpect(jsonPath("$.steps[5].type").value("MODEL_REQUEST"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var sessionId = UUID.fromString(objectMapper.readTree(startResponse).get("sessionId").asText());

        mockMvc.perform(get("/api/v1/agent-runtime/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVariantKey").value("personalized-smoke"))
                .andExpect(jsonPath("$.finalOutput.seenLearnerLevel").value("HSK2"));
    }

    @Test
    void hsk5LessonGeneratorRunsProfileAndTeacherPreGenerationStepsInTrace() throws Exception {
        var source = Map.of(
                "id", "source-1",
                "type", "TEXT_NOTE",
                "position", 0,
                "textContent", "现代生活节奏很快，我们需要保持平衡。");
        var draft = Map.of(
                "id", "draft-1",
                "title", "口语结构练习",
                "description", "HSK5 practice",
                "userInstructions", "Make it conversational",
                "explanationLanguage", "ru",
                "translationLanguage", "en",
                "sources", List.of(source));
        var input = Map.of(
                "draftId", "draft-1",
                "moduleKey", "hsk5_v1",
                "moduleSchemaVersion", 1,
                "draft", draft,
                "orderedSources", List.of(source));
        var payload = objectMapper.writeValueAsString(new StartPayload(
                "lesson-generator:hsk5_v1",
                "fake-model",
                "Generate a lesson JSON from the provided lesson draft.",
                input,
                null));

        mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileKey").value("lesson-generator:hsk5_v1"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps[1].type").value("PRE_GENERATION_STARTED"))
                .andExpect(jsonPath("$.steps[2].type").value("PRE_GENERATION_STEP"))
                .andExpect(jsonPath("$.steps[2].payload.stepKey").value("learner-profile-context"))
                .andExpect(jsonPath("$.steps[2].payload.emittedSectionTitles[0]").value("Learner profile context"))
                .andExpect(jsonPath("$.steps[2].payload.emittedArtifactKeys[0]").value("learnerProfileContext"))
                .andExpect(jsonPath("$.steps[3].type").value("PRE_GENERATION_STEP"))
                .andExpect(jsonPath("$.steps[3].payload.stepKey").value("teacher-personality-context"))
                .andExpect(jsonPath("$.steps[3].payload.emittedSectionTitles[0]").value("Teacher personality context"))
                .andExpect(jsonPath("$.steps[3].payload.emittedArtifactKeys[0]").value("teacherPersonalityContext"))
                .andExpect(jsonPath("$.steps[4].type").value("PRE_GENERATION_STEP"))
                .andExpect(jsonPath("$.steps[4].payload.stepKey").value("lesson-vocabulary-review-plan"))
                .andExpect(jsonPath("$.steps[5].type").value("PRE_GENERATION_COMPLETED"));
    }

    @Test
    void startReturnsBadRequestForUnknownWorkflowVariant() throws Exception {
        var payload = objectMapper.writeValueAsString(new StartPayload(
                "test-agent:v1", "fake-model", "Do x", Map.of("objective", "x"), "missing-variant"));

        mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    private void assertCount(String sql, int expected) {
        var actual = jdbcTemplate.queryForObject(sql, Integer.class);
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }

    private void assertText(String sql, String expected) {
        var actual = jdbcTemplate.queryForObject(sql, String.class);
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }

    private record StartPayload(
            String profileKey, String modelKey, String task, Map<String, Object> input, String workflowVariantKey) {}
}
