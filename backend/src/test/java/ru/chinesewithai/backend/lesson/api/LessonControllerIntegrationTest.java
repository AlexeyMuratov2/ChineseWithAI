package ru.chinesewithai.backend.lesson.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class LessonControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM lesson_vocabulary_items");
        jdbcTemplate.update("DELETE FROM learner_vocabulary_progress");
        jdbcTemplate.update("DELETE FROM lessons");
        jdbcTemplate.update("DELETE FROM agent_steps");
        jdbcTemplate.update("DELETE FROM agent_sessions");
        jdbcTemplate.update("DELETE FROM lesson_draft_sources");
        jdbcTemplate.update("DELETE FROM lesson_drafts");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void listLessonModulesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/modules")).andExpect(status().isUnauthorized());
    }

    @Test
    void listLessonModulesReturnsCatalogEntries() throws Exception {
        register("lesson_modules_reader", "StrongPass123!", "Lesson Modules Reader");
        var token = login("lesson_modules_reader", "StrongPass123!");

        var response = mockMvc.perform(get("/api/v1/lessons/modules").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var modules = objectMapper.readTree(response);
        assertThat(modules).hasSize(2);
        assertThat(modules).extracting(node -> node.path("moduleKey").asText()).contains("TestModule", "hsk5_v1");
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
                .andExpect(jsonPath("$.content.reviewWords").isArray())
                .andExpect(jsonPath("$.content.sections[0].type").value("word_usage"))
                .andExpect(jsonPath("$.content.sections[1].type").value("reading"));
    }

    @Test
    void generateLessonFromDraftRepairsInvalidAgentOutputAndCreatesLesson() throws Exception {
        register("lesson_repair", "StrongPass123!", "Lesson Repair");
        var token = login("lesson_repair", "StrongPass123!");
        var draftId = createDraftWithSingleTextSource(
                token, "Repairable output", "[[REPAIRABLE_INVALID_LESSON_OUTPUT]]");

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
                .andExpect(jsonPath("$.content.reviewWords").isArray())
                .andExpect(jsonPath("$.content.sections[0].type").value("word_usage"))
                .andExpect(jsonPath("$.content.sections[1].type").value("reading"));
    }

    @Test
    void repeatedGenerateHsk5UsesModuleProfileDefaultWorkflowAndReviewBlocks() throws Exception {
        register("lesson_hsk5_generate", "StrongPass123!", "Lesson HSK5 Generate");
        var token = login("lesson_hsk5_generate", "StrongPass123!");

        var firstDraftId = createDraftWithSingleTextSource(
                token,
                "第一次 HSK5",
                "机会常常来自准备，也会影响一个人的选择。");
        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", firstDraftId,
                                "moduleKey", "hsk5_v1",
                                "modelKey", "fake-model"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleKey").value("hsk5_v1"))
                .andExpect(jsonPath("$.content.sections[2].text").value("机会常常来自准备，也会影响一个人的选择。"));

        var secondDraftText = "保持清楚的态度，才能理解复杂的问题。";
        var secondDraftId = createDraftWithSingleTextSource(token, "第二次 HSK5", secondDraftText);
        var secondGenerateResponse = mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", secondDraftId,
                                "moduleKey", "hsk5_v1",
                                "modelKey", "fake-model"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleKey").value("hsk5_v1"))
                .andExpect(jsonPath("$.generatorSessionId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var generatedLesson = objectMapper.readTree(secondGenerateResponse);
        var content = generatedLesson.path("content");
        assertThat(content.path("reviewWords")).hasSize(2);
        assertThat(content.path("sections")).extracting(section -> section.path("type").asText())
                .contains("word_study", "text", "conversation", "word_game");
        var textSections = new java.util.ArrayList<JsonNode>();
        var wordStudyStatuses = new java.util.ArrayList<String>();
        content.path("sections").forEach(section -> {
            if ("text".equals(section.path("type").asText())) {
                textSections.add(section);
            }
            if ("word_study".equals(section.path("type").asText())) {
                wordStudyStatuses.add(section.path("vocabularyStatus").asText());
            }
        });
        assertThat(textSections).singleElement().satisfies(section -> {
            assertThat(section.path("text").asText()).isEqualTo(secondDraftText);
            assertThat(section.has("translation")).isFalse();
        });
        assertThat(wordStudyStatuses).contains("review");

        var generatorSessionId = UUID.fromString(generatedLesson.get("generatorSessionId").asText());
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT profile_key FROM agent_sessions WHERE id = ?",
                        String.class,
                        generatorSessionId))
                .isEqualTo("lesson-generator:hsk5_v1");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT workflow_variant_key FROM agent_sessions WHERE id = ?",
                        String.class,
                        generatorSessionId))
                .isNull();

        var stepKeys = preGenerationStepKeys(generatorSessionId);
        assertThat(stepKeys)
                .containsExactly(
                        "current-user-profile",
                        "learner-profile-context",
                        "teacher-personality-context",
                        "lesson-vocabulary-review-plan");
    }

    @Test
    void generateHsk5RepairsInvalidAgentOutputAndCreatesLesson() throws Exception {
        register("lesson_hsk5_repair", "StrongPass123!", "Lesson HSK5 Repair");
        var token = login("lesson_hsk5_repair", "StrongPass123!");
        var draftId = createDraftWithSingleTextSource(
                token, "Repairable HSK5 output", "[[REPAIRABLE_INVALID_LESSON_OUTPUT]]");

        var generateResponse = mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", draftId,
                                "moduleKey", "hsk5_v1",
                                "modelKey", "fake-model"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleKey").value("hsk5_v1"))
                .andExpect(jsonPath("$.generatorSessionId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var generatedLesson = objectMapper.readTree(generateResponse);
        var generatorSessionId = UUID.fromString(generatedLesson.get("generatorSessionId").asText());
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM agent_steps WHERE session_id = ? AND step_type = 'OUTPUT_VALIDATION_FAILED'",
                        Integer.class,
                        generatorSessionId))
                .isEqualTo(1);
        assertThat(generatedLesson.path("content").path("sections"))
                .extracting(section -> section.path("type").asText())
                .contains("word_study", "text", "conversation", "word_game");
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

    @Test
    void manualCreatePersistsLessonVocabularyAndLearnerProgress() throws Exception {
        register("lesson_vocab_manual", "StrongPass123!", "Lesson Vocabulary Manual");
        var token = login("lesson_vocab_manual", "StrongPass123!");
        var userId = userIdForUsername("lesson_vocab_manual");

        var createResponse = mockMvc.perform(post("/api/v1/lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "moduleKey", "TestModule",
                                "content", validTestModuleLessonJson()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createdLesson = objectMapper.readTree(createResponse);
        var lessonId = UUID.fromString(createdLesson.get("id").asText());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM lesson_vocabulary_items WHERE lesson_id = ?",
                        Integer.class,
                        lessonId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learner_vocabulary_progress WHERE user_id = ?",
                        Integer.class,
                        userId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learner_vocabulary_progress WHERE user_id = ? AND status = 'NEW'",
                        Integer.class,
                        userId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learner_vocabulary_progress WHERE user_id = ? AND last_reviewed_at IS NULL",
                        Integer.class,
                        userId))
                .isEqualTo(2);
    }

    @Test
    void repeatedGenerateUsesVocabularyReviewPreGenerationWorkflowAndPersistsTrace() throws Exception {
        register("lesson_vocab_generate", "StrongPass123!", "Lesson Vocabulary Generate");
        var token = login("lesson_vocab_generate", "StrongPass123!");
        var userId = userIdForUsername("lesson_vocab_generate");

        var firstDraftId = createDraftWithSingleTextSource(token, "First generate", "Source one");
        mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", firstDraftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isCreated());

        var secondDraftId = createDraftWithSingleTextSource(token, "Second generate", "Source two");
        var secondGenerateResponse = mockMvc.perform(post("/api/v1/lessons/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", secondDraftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var generatedLesson = objectMapper.readTree(secondGenerateResponse);
        var reviewWordTranslations = new java.util.ArrayList<String>();
        generatedLesson.path("content").path("reviewWords").forEach(node -> {
            reviewWordTranslations.add(node.path("translation").asText());
        });
        assertThat(reviewWordTranslations).containsExactlyInAnyOrder("to know", "to study");
        var generatorSessionId = UUID.fromString(generatedLesson.get("generatorSessionId").asText());
        var lessonId = UUID.fromString(generatedLesson.get("id").asText());

        var stepPayloads = jdbcTemplate.query(
                "SELECT step_type, payload_json FROM agent_steps WHERE session_id = ? ORDER BY step_index",
                (rs, rowNum) -> new AgentStepRow(rs.getString("step_type"), rs.getString("payload_json")),
                generatorSessionId);

        var startedPayload = payloadOf(stepPayloads, "PRE_GENERATION_STARTED");
        var stepPayload = payloadOf(stepPayloads, "PRE_GENERATION_STEP");
        var completedPayload = payloadOf(stepPayloads, "PRE_GENERATION_COMPLETED");

        assertThat(startedPayload.path("resolvedWorkflowVariantKey").asText())
                .isEqualTo("draft-generation-with-review:v1");
        assertThat(stepPayload.path("stepKey").asText()).isEqualTo("lesson-vocabulary-review-plan");
        assertThat(stepPayload.path("emittedArtifactKeys")).extracting(JsonNode::asText).contains("vocabularyReviewPlan");
        assertThat(completedPayload.path("artifactKeys")).extracting(JsonNode::asText).contains("vocabularyReviewPlan");

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM lesson_vocabulary_items WHERE lesson_id = ?",
                        Integer.class,
                        lessonId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learner_vocabulary_progress WHERE user_id = ?",
                        Integer.class,
                        userId))
                .isEqualTo(2);
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

    private UUID userIdForUsername(String username) {
        return UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT id::text FROM app_user WHERE username = ?", String.class, username));
    }

    private JsonNode payloadOf(List<AgentStepRow> steps, String stepType) throws Exception {
        return objectMapper.readTree(steps.stream()
                .filter(step -> step.stepType().equals(stepType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing step: " + stepType))
                .payloadJson());
    }

    private List<String> preGenerationStepKeys(UUID generatorSessionId) {
        return jdbcTemplate.query(
                "SELECT payload_json FROM agent_steps WHERE session_id = ? AND step_type = 'PRE_GENERATION_STEP' ORDER BY step_index",
                (rs, rowNum) -> {
                    try {
                        return objectMapper.readTree(rs.getString("payload_json")).path("stepKey").asText();
                    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                        throw new IllegalStateException("Failed to parse step payload", ex);
                    }
                },
                generatorSessionId);
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

    private record AgentStepRow(String stepType, String payloadJson) {}
}
