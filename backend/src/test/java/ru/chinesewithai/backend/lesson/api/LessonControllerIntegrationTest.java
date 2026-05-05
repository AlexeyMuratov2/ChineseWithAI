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
        jdbcTemplate.update("DELETE FROM lesson_generation_run_stages");
        jdbcTemplate.update("DELETE FROM lesson_generation_runs");
        jdbcTemplate.update("DELETE FROM lesson_vocabulary_items");
        jdbcTemplate.update("DELETE FROM learner_vocabulary_progress");
        jdbcTemplate.update("DELETE FROM lessons");
        jdbcTemplate.update("DELETE FROM agent_steps");
        jdbcTemplate.update("DELETE FROM agent_sessions");
        jdbcTemplate.update("DELETE FROM lesson_draft_sources");
        jdbcTemplate.update("DELETE FROM lesson_drafts");
    }

    @Test
    void listLessonModulesReturnsCatalogEntries() throws Exception {
        var response = mockMvc.perform(get("/api/v1/lessons/modules"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var modules = objectMapper.readTree(response);
        assertThat(modules).hasSize(2);
        assertThat(modules).extracting(node -> node.path("moduleKey").asText()).contains("TestModule", "hsk5_v1");
    }

    @Test
    void listLessonsByModuleReturnsLessonsForThatModule() throws Exception {

        var firstCreateResponse = mockMvc.perform(post("/api/v1/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "moduleKey", "TestModule",
                                "content", validTestModuleLessonJson()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/v1/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "moduleKey", "hsk5_v1",
                                "content", objectMapper.valueToTree(Map.of(
                                        "schemaVersion", 1,
                                        "moduleKey", "hsk5_v1",
                                        "title", "HSK5 lesson",
                                        "studyLanguage", "zh",
                                        "explanationLanguage", "zh",
                                        "translationLanguage", "en",
                                        "newWords", java.util.List.of(),
                                        "sections", java.util.List.of()))))))
                .andExpect(status().isBadRequest());

        var secondCreateResponse = mockMvc.perform(post("/api/v1/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "moduleKey", "TestModule",
                                "content", validTestModuleLessonJson()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var firstLessonId = objectMapper.readTree(firstCreateResponse).get("id").asText();
        var secondLessonId = objectMapper.readTree(secondCreateResponse).get("id").asText();

        var response = mockMvc.perform(get("/api/v1/lessons/modules/{moduleKey}", "TestModule"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var lessons = objectMapper.readTree(response);
        assertThat(lessons).hasSize(2);
        assertThat(lessons).extracting(node -> node.path("moduleKey").asText()).containsOnly("TestModule");
        assertThat(lessons).extracting(node -> node.path("id").asText()).containsExactly(secondLessonId, firstLessonId);
    }

    @Test
    void manualCreateAndGetReturnStoredLessonJson() throws Exception {
        var lessonJson = validTestModuleLessonJson();

        var createPayload = objectMapper.writeValueAsString(Map.of("moduleKey", "TestModule", "content", lessonJson));
        var createResponse = mockMvc.perform(post("/api/v1/lessons")
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

        var getResponse = mockMvc.perform(get("/api/v1/lessons/{lessonId}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lessonId.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThatJsonEquals(created.get("content"), objectMapper.readTree(getResponse).get("content"));
    }

    @Test
    void manualCreateRejectsInvalidContract() throws Exception {

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateLessonFromDraftCreatesStoredLesson() throws Exception {
        var draftId = createDraftWithSingleTextSource(
                "Generate test",
                "New words: renshi, xuexi\nShort text: I know this teacher, so I study Chinese every day.");

        var generatePayload = objectMapper.writeValueAsString(Map.of(
                "draftId", draftId,
                "moduleKey", "TestModule",
                "modelKey", "fake-model"));

        mockMvc.perform(post("/api/v1/lessons/generate")
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
        var draftId = createDraftWithSingleTextSource(
                "Repairable output", "[[REPAIRABLE_INVALID_LESSON_OUTPUT]]");

        var generatePayload = objectMapper.writeValueAsString(Map.of(
                "draftId", draftId,
                "moduleKey", "TestModule",
                "modelKey", "fake-model"));

        mockMvc.perform(post("/api/v1/lessons/generate")
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

        var firstDraftId = createDraftWithSingleTextSource(
                "First HSK5",
                "Opportunity often comes from preparation.");
        var firstGenerateResponse = mockMvc.perform(post("/api/v1/lessons/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", firstDraftId,
                                "moduleKey", "hsk5_v1",
                                "modelKey", "fake-model"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleKey").value("hsk5_v1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertContainsTextSection(
                objectMapper.readTree(firstGenerateResponse).path("content"),
                "Opportunity often comes from preparation.");

        var secondDraftText = "Keeping a clear attitude helps solve complex problems.";
        var secondDraftId = createDraftWithSingleTextSource("Second HSK5", secondDraftText);
        var secondGenerateResponse = mockMvc.perform(post("/api/v1/lessons/generate")
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
                .contains("word_study", "grammar", "text", "conversation", "word_game");
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
                .isEqualTo("lesson-generator:hsk5_v1_composer");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT workflow_variant_key FROM agent_sessions WHERE id = ?",
                        String.class,
                        generatorSessionId))
                .isNull();

        var lessonId = UUID.fromString(generatedLesson.get("id").asText());
        var runId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT id::text FROM lesson_generation_runs WHERE lesson_id = ?",
                String.class,
                lessonId));
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT final_generator_session_id::text FROM lesson_generation_runs WHERE id = ?",
                        String.class,
                        runId))
                .isEqualTo(generatorSessionId.toString());
        assertThat(generationStageKeys(runId))
                .containsExactly("blueprint", "grammar", "vocabulary_practice", "word_game", "composer");

        var blueprintSessionId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT agent_session_id::text FROM lesson_generation_run_stages WHERE run_id = ? AND stage_key = 'blueprint'",
                String.class,
                runId));
        var stepKeys = preGenerationStepKeys(blueprintSessionId);
        assertThat(stepKeys)
                .containsExactly(
                        "learner-profile-context",
                        "teacher-personality-context",
                        "lesson-vocabulary-review-plan");

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learner_vocabulary_progress WHERE last_reviewed_at IS NOT NULL AND review_count > 0",
                        Integer.class))
                .isEqualTo(2);
    }

    @Test
    void generateHsk5RepairsInvalidAgentOutputAndCreatesLesson() throws Exception {
        var draftId = createDraftWithSingleTextSource(
                "Repairable HSK5 output", "[[REPAIRABLE_INVALID_LESSON_OUTPUT]]");

        var generateResponse = mockMvc.perform(post("/api/v1/lessons/generate")
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
                .contains("word_study", "grammar", "text", "conversation", "word_game");
    }

    @Test
    void generateRejectsMissingAndUnknownModuleKey() throws Exception {
        var draftId = createDraftWithSingleTextSource("Missing module", "text");

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("draftId", draftId, "moduleKey", ""))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("draftId", draftId, "moduleKey", "MissingModule"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateRejectsUnsupportedDraftShape() throws Exception {
        var multiSourceDraftId = createDraftWithSources("Two sources", true, false);
        var documentDraftId = createDraftWithSources("Document source", false, true);

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", multiSourceDraftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", documentDraftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateReturnsUnprocessableEntityWhenAgentOutputViolatesContract() throws Exception {
        var draftId = createDraftWithSingleTextSource("Invalid output", "[[INVALID_LESSON_OUTPUT]]");

        mockMvc.perform(post("/api/v1/lessons/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", draftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void manualCreatePersistsLessonVocabularyAndLearnerProgress() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/lessons")
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
                        "SELECT COUNT(*) FROM learner_vocabulary_progress",
                        Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learner_vocabulary_progress WHERE status = 'NEW'",
                        Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learner_vocabulary_progress WHERE last_reviewed_at IS NULL",
                        Integer.class))
                .isEqualTo(2);
    }

    @Test
    void repeatedGenerateUsesVocabularyReviewPreGenerationWorkflowAndPersistsTrace() throws Exception {
        var firstDraftId = createDraftWithSingleTextSource("First generate", "Source one");
        mockMvc.perform(post("/api/v1/lessons/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "draftId", firstDraftId,
                                "moduleKey", "TestModule",
                                "modelKey", "fake-model"))))
                .andExpect(status().isCreated());

        var secondDraftId = createDraftWithSingleTextSource("Second generate", "Source two");
        var secondGenerateResponse = mockMvc.perform(post("/api/v1/lessons/generate")
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
                        "SELECT COUNT(*) FROM learner_vocabulary_progress",
                        Integer.class))
                .isEqualTo(2);
    }

    private UUID createDraftWithSingleTextSource(String title, String textContent) throws Exception {
        var draftId = createDraft(title);
        addTextSource(draftId, textContent);
        return draftId;
    }

    private UUID createDraftWithSources(String title, boolean addSecondTextSource, boolean addDocumentSource)
            throws Exception {
        var draftId = createDraft(title);
        addTextSource(draftId, "primary source");
        if (addSecondTextSource) {
            addTextSource(draftId, "secondary source");
        }
        if (addDocumentSource) {
            var payload = objectMapper.writeValueAsString(Map.of(
                    "type", "DOCUMENT_FILE",
                    "documentFileId", UUID.randomUUID(),
                    "documentOriginalFileName", "source.pdf"));
            mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
        }
        return draftId;
    }

    private UUID createDraft(String title) throws Exception {
        var payload = objectMapper.writeValueAsString(Map.of("title", title));
        var response = mockMvc.perform(post("/api/v1/lesson-drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void addTextSource(UUID draftId, String textContent) throws Exception {
        var payload = objectMapper.writeValueAsString(Map.of("type", "TEXT_NOTE", "textContent", textContent));
        mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private JsonNode validTestModuleLessonJson() {
        return objectMapper.valueToTree(Map.of(
                "schemaVersion", 1,
                "moduleKey", "TestModule",
                "title", "Recognizing and studying",
                "studyLanguage", "zh",
                "explanationLanguage", "zh",
                "translationLanguage", "en",
                "newWords", java.util.List.of(
                        Map.of("word", "renshi", "pinyin", "renshi", "translation", "to know"),
                        Map.of("word", "xuexi", "pinyin", "xuexi", "translation", "to study")),
                "sections", java.util.List.of(
                        Map.of(
                                "type", "word_usage",
                                "title", "New words",
                                "items", java.util.List.of(
                                        Map.of("word", "renshi", "sentence", "I know this teacher.", "translation", "I know this teacher."),
                                        Map.of("word", "xuexi", "sentence", "I study Chinese every day.", "translation", "I study Chinese every day."))),
                        Map.of(
                                "type", "reading",
                                "title", "Reading",
                                "text", "I know this teacher, so I study Chinese with him every day.",
                                "translation", "I know this teacher, so I study Chinese with him every day."))));
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

    private List<String> generationStageKeys(UUID runId) {
        return jdbcTemplate.query(
                "SELECT stage_key FROM lesson_generation_run_stages WHERE run_id = ? ORDER BY stage_index",
                (rs, rowNum) -> rs.getString("stage_key"),
                runId);
    }

    private void assertContainsTextSection(JsonNode content, String expectedText) {
        var textSections = new java.util.ArrayList<JsonNode>();
        content.path("sections").forEach(section -> {
            if ("text".equals(section.path("type").asText())) {
                textSections.add(section);
            }
        });
        assertThat(textSections).anySatisfy(section -> assertThat(section.path("text").asText()).isEqualTo(expectedText));
    }

    private static void assertThatJsonEquals(JsonNode expected, JsonNode actual) {
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }

    private record AgentStepRow(String stepType, String payloadJson) {}
}
