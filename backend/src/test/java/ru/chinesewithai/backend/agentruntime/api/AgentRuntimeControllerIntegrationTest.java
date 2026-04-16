package ru.chinesewithai.backend.agentruntime.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
        jdbcTemplate.update("DELETE FROM agent_steps");
        jdbcTemplate.update("DELETE FROM agent_sessions");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void startRequiresAuthentication() throws Exception {
        var payload = objectMapper.writeValueAsString(new StartPayload(
                "test-agent:v1", "fake-model", "Run a smoke test", Map.of("objective", "smoke-test")));

        mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startAndGetSessionReturnCompletedTraceAndPersistedState() throws Exception {
        register("runtime_owner", "StrongPass123!", "Runtime Owner");
        var ownerToken = login("runtime_owner", "StrongPass123!");

        var startPayload = objectMapper.writeValueAsString(new StartPayload(
                "test-agent:v1", "fake-model", "Run a smoke test", Map.of("objective", "smoke-test")));

        var startResponse = mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
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
                .andExpect(jsonPath("$.steps.length()").value(11))
                .andExpect(jsonPath("$.steps[0].type").value("SESSION_CREATED"))
                .andExpect(jsonPath("$.steps[10].type").value("SESSION_COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var sessionId = UUID.fromString(objectMapper.readTree(startResponse).get("sessionId").asText());

        mockMvc.perform(get("/api/v1/agent-runtime/sessions/{sessionId}", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.modelKey").value("fake-model"))
                .andExpect(jsonPath("$.task").value("Run a smoke test"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.steps.length()").value(11));

        assertCount("SELECT COUNT(*) FROM agent_sessions", 1);
        assertCount("SELECT COUNT(*) FROM agent_steps", 11);
        assertText("SELECT model_key FROM agent_sessions", "fake-model");
        assertText("SELECT task FROM agent_sessions", "Run a smoke test");

        register("runtime_other", "StrongPass123!", "Runtime Other");
        var otherToken = login("runtime_other", "StrongPass123!");
        mockMvc.perform(get("/api/v1/agent-runtime/sessions/{sessionId}", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void startReturnsNotFoundForUnknownProfile() throws Exception {
        register("runtime_missing", "StrongPass123!", "Runtime Missing");
        var token = login("runtime_missing", "StrongPass123!");
        var payload = objectMapper.writeValueAsString(
                new StartPayload("missing-profile", "fake-model", "Do x", Map.of("objective", "x")));

        mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void startReturnsNotFoundForUnknownModel() throws Exception {
        register("runtime_missing_model", "StrongPass123!", "Runtime Missing Model");
        var token = login("runtime_missing_model", "StrongPass123!");
        var payload = objectMapper.writeValueAsString(
                new StartPayload("test-agent:v1", "missing-model", "Do x", Map.of("objective", "x")));

        mockMvc.perform(post("/api/v1/agent-runtime/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    private void assertCount(String sql, int expected) {
        var actual = jdbcTemplate.queryForObject(sql, Integer.class);
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }

    private void assertText(String sql, String expected) {
        var actual = jdbcTemplate.queryForObject(sql, String.class);
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }

    private void register(String username, String password, String displayName) throws Exception {
        var payload = objectMapper.writeValueAsString(new RegisterPayload(username, password, displayName));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        var payload = objectMapper.writeValueAsString(new LoginPayload(username, password));
        var response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record RegisterPayload(String username, String password, String displayName) {}

    private record LoginPayload(String username, String password) {}

    private record StartPayload(String profileKey, String modelKey, String task, Map<String, Object> input) {}
}
