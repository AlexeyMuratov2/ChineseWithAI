package ru.chinesewithai.backend.grammarexercise.api;

import static org.assertj.core.api.Assertions.assertThat;
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
class GrammarExerciseControllerIntegrationTest extends AbstractIntegrationTest {

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
    void generateRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/grammar-exercises/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateReturnsGrammarExerciseContentAndPersistsGeneratorSession() throws Exception {
        register("grammar_generate", "StrongPass123!", "Grammar Generate");
        var token = login("grammar_generate", "StrongPass123!");

        var response = mockMvc.perform(post("/api/v1/grammar-exercises/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generatorSessionId").isNotEmpty())
                .andExpect(jsonPath("$.content.schemaVersion").value(1))
                .andExpect(jsonPath("$.content.explanationLanguage").value("zh"))
                .andExpect(jsonPath("$.content.explanations[0].title").value("yu"))
                .andExpect(jsonPath("$.content.exercises.length()").value(2))
                .andExpect(jsonPath("$.content.exercises[0].type").value("complete_sentence"))
                .andExpect(jsonPath("$.content.exercises[1].type").value("choose_word"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var generatorSessionId =
                UUID.fromString(objectMapper.readTree(response).get("generatorSessionId").asText());
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT profile_key FROM agent_sessions WHERE id = ?",
                        String.class,
                        generatorSessionId))
                .isEqualTo("grammar-exercise-generator:v1");
    }

    @Test
    void generateDefaultsExplanationLanguageToChinese() throws Exception {
        register("grammar_default_language", "StrongPass123!", "Grammar Default Language");
        var token = login("grammar_default_language", "StrongPass123!");
        var payload = Map.of(
                "modelKey", "fake-model",
                "items", List.of(Map.of("term", "yu", "focus", "explain usage")));

        mockMvc.perform(post("/api/v1/grammar-exercises/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content.explanationLanguage").value("zh"));
    }

    @Test
    void generateRejectsInvalidRequestShape() throws Exception {
        register("grammar_invalid_request", "StrongPass123!", "Grammar Invalid Request");
        var token = login("grammar_invalid_request", "StrongPass123!");

        mockMvc.perform(post("/api/v1/grammar-exercises/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("modelKey", "fake-model", "items", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateReturnsUnprocessableEntityWhenAgentOutputStaysInvalid() throws Exception {
        register("grammar_invalid_output", "StrongPass123!", "Grammar Invalid Output");
        var token = login("grammar_invalid_output", "StrongPass123!");
        var payload = Map.of(
                "modelKey",
                "fake-model",
                "items",
                List.of(Map.of(
                        "term",
                        "yu",
                        "focus",
                        "explain usage [[INVALID_GRAMMAR_EXERCISE_OUTPUT]]")));

        mockMvc.perform(post("/api/v1/grammar-exercises/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity());
    }

    private Map<String, Object> validPayload() {
        return Map.of(
                "explanationLanguage",
                "zh",
                "modelKey",
                "fake-model",
                "items",
                List.of(
                        Map.of("term", "yu", "focus", "explain all usage scenarios"),
                        Map.of("term", "dating / xunwen", "focus", "compare usage")));
    }

    private void register(String username, String password, String displayName) throws Exception {
        var payload = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "displayName", displayName));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        var payload = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
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
}
