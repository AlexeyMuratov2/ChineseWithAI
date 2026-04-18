package ru.chinesewithai.backend.lesson.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class LessonControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM lessons");
        jdbcTemplate.update("DELETE FROM agent_steps");
        jdbcTemplate.update("DELETE FROM agent_sessions");
        jdbcTemplate.update("DELETE FROM lesson_draft_sources");
        jdbcTemplate.update("DELETE FROM lesson_drafts");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void manualCreateAndGetReturnStoredLessonJson() throws Exception {
        register("lesson_owner", "StrongPass123!", "Lesson Owner");
        var token = login("lesson_owner", "StrongPass123!");
        var lessonJson = validTestModuleLessonJson();

        var createPayload = objectMapper.writeValueAsString(Map.of("moduleKey", "TestModule", "content", lessonJson));
        var createResponse = mockMvc.perform(post("/api/v1/lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleKey").value("TestModule"))
                .andExpect(jsonPath("$.content.moduleKey").value("TestModule"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var created = objectMapper.readTree(createResponse);
        var lessonId = UUID.fromString(created.get("id").asText());

        var getResponse = mockMvc.perform(get("/api/v1/lessons/{lessonId}", lessonId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lessonId.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThatJsonEquals(created.get("content"), objectMapper.readTree(getResponse).get("content"));
    }

    @Test
    void manualCreateRejectsInvalidContract() throws Exception {
        register("lesson_invalid", "StrongPass123!", "Lesson Invalid");
        var token = login("lesson_invalid", "StrongPass123!");

        var invalidPayload = objectMapper.writeValueAsString(Map.of(
                "moduleKey",
                "TestModule",
                "content",
                Map.of(
                        "schemaVersion", 1,
                        "moduleKey", "TestModule",
                        "title", "Broken",
                        "studyLanguage", "zh",
                        "explanationLanguage", "zh",
                        "translationLanguage", "en",
                        "newWords", java.util.List.of())));

        mockMvc.perform(post("/api/v1/lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateLessonFromDraftCreatesStoredLesson() throws Exception {
        register("lesson_generate", "StrongPass123!", "Lesson Generate");
        var token = login("lesson_generate", "StrongPass123!");
        var draftId = createDraftWithSingleTextSource(token, "Generate test", "新词: 认识, 学习\n短文: 我认识这个老师，所以我每天跟他学习中文。");

        var generatePayload = objectMapper.writeValueAsString(Map.of(
                "draftId", draftId,
                "moduleKey", "TestModule",
                "modelKey", "fake-model"));

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generatePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleKey").value("TestModule"))
                .andExpect(jsonPath("$.generatorSessionId").isNotEmpty())
                .andExpect(jsonPath("$.content.sections[0].type").value("word_usage"))
                .andExpect(jsonPath("$.content.sections[1].type").value("reading"));
    }

    @Test
    void generateRejectsMissingAndUnknownModuleKey() throws Exception {
        register("lesson_missing_module", "StrongPass123!", "Lesson Missing Module");
        var token = login("lesson_missing_module", "StrongPass123!");
        var draftId = createDraftWithSingleTextSource(token, "Missing module", "text");

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("draftId", draftId, "moduleKey", ""))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("draftId", draftId, "moduleKey", "MissingModule"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateRejectsUnsupportedDraftShape() throws Exception {
        register("lesson_invalid_draft", "StrongPass123!", "Lesson Invalid Draft");
        var token = login("lesson_invalid_draft", "StrongPass123!");
        var multiSourceDraftId = createDraftWithSources(token, "Two sources", true, false);
        var documentDraftId = createDraftWithSources(token, "Document source", false, true);

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", multiSourceDraftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", documentDraftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateReturnsUnprocessableEntityWhenAgentOutputViolatesContract() throws Exception {
        register("lesson_invalid_output", "StrongPass123!", "Lesson Invalid Output");
        var token = login("lesson_invalid_output", "StrongPass123!");
        var draftId = createDraftWithSingleTextSource(token, "Invalid output", "[[INVALID_LESSON_OUTPUT]]");

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", draftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isUnprocessableEntity());
    }

    private UUID createDraftWithSingleTextSource(String token, String title, String textContent) throws Exception {
        var draftId = createDraft(token, title);
        addTextSource(token, draftId, textContent);
        return draftId;
    }

    private UUID createDraftWithSources(String token, String title, boolean addSecondTextSource, boolean addDocumentSource)
            throws Exception {
        var draftId = createDraft(token, title);
        addTextSource(token, draftId, "primary source");
        if (addSecondTextSource) {
            addTextSource(token, draftId, "secondary source");
        }
        if (addDocumentSource) {
            var payload = objectMapper.writeValueAsString(Map.of(
                    "type", "DOCUMENT_FILE",
                    "documentFileId", UUID.randomUUID(),
                    "documentOriginalFileName", "source.pdf"));
            mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
        }
        return draftId;
    }

    private UUID createDraft(String token, String title) throws Exception {
        var payload = objectMapper.writeValueAsString(Map.of("title", title));
        var response = mockMvc.perform(post("/api/v1/lesson-drafts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void addTextSource(String token, UUID draftId, String textContent) throws Exception {
        var payload = objectMapper.writeValueAsString(Map.of("type", "TEXT_NOTE", "textContent", textContent));
        mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private JsonNode validTestModuleLessonJson() {
        return objectMapper.valueToTree(Map.of(
                "schemaVersion", 1,
                "moduleKey", "TestModule",
                "title", "认识和学习",
                "studyLanguage", "zh",
                "explanationLanguage", "zh",
                "translationLanguage", "en",
                "newWords", java.util.List.of(
                        Map.of("word", "认识", "pinyin", "rènshi", "translation", "to know"),
                        Map.of("word", "学习", "pinyin", "xuéxí", "translation", "to study")),
                "sections", java.util.List.of(
                        Map.of(
                                "type", "word_usage",
                                "title", "先看新词",
                                "items", java.util.List.of(
                                        Map.of("word", "认识", "sentence", "我认识这个老师。", "translation", "I know this teacher."),
                                        Map.of("word", "学习", "sentence", "我每天学习中文。", "translation", "I study Chinese every day."))),
                        Map.of(
                                "type", "reading",
                                "title", "短文",
                                "text", "我认识这个老师，所以我每天跟他学习中文。",
                                "translation", "I know this teacher, so I study Chinese with him every day."))));
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

    private static void assertThatJsonEquals(JsonNode expected, JsonNode actual) {
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
