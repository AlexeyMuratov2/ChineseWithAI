package ru.chinesewithai.backend.lessondraft.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
import ru.chinesewithai.backend.storedfile.api.StoredFileController;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LessonDraftControllerIntegrationTest extends AbstractIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM file_upload_sessions");
        jdbcTemplate.update("DELETE FROM stored_files");
        jdbcTemplate.update("DELETE FROM lesson_draft_sources");
        jdbcTemplate.update("DELETE FROM lesson_drafts");
    }

    @Test
    void listReturnsEmptyPageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/lesson-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void fullFlowSupportsCrudAndSources() throws Exception {
        var createPayload = objectMapper.writeValueAsString(new CreateDraftPayload("HSK 3 travel", "trip prep", "focus on dialogs", null, null));
        var createResponse = mockMvc.perform(post("/api/v1/lesson-drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.explanationLanguage").value("zh"))
                .andExpect(jsonPath("$.translationLanguage").value("en"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var created = objectMapper.readTree(createResponse);
        var draftId = UUID.fromString(created.get("id").asText());

        var addTextPayload = objectMapper.writeValueAsString(
                new AddSourcePayload("TEXT_NOTE", "Need practical airport phrases", null, null));
        var afterText = mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addTextPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var textSourceId = UUID.fromString(objectMapper.readTree(afterText).get("sources").get(0).get("id").asText());

        var documentFileId = uploadTextFile("airport-handbook.txt", "机场见面以后，我们先确认登机口，然后再买咖啡。");
        var addDocPayload = objectMapper.writeValueAsString(
                new AddSourcePayload("DOCUMENT_FILE", null, documentFileId, "airport-handbook.txt"));
        var afterDoc = mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addDocPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources.length()").value(2))
                .andExpect(jsonPath("$.sources[1].textContent").value("机场见面以后，我们先确认登机口，然后再买咖啡。"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var afterDocJson = objectMapper.readTree(afterDoc);
        var docSourceId = findSourceIdByType(afterDocJson, "DOCUMENT_FILE");

        var reorderPayload = objectMapper.writeValueAsString(new ReorderSourcesPayload(List.of(docSourceId, textSourceId)));
        mockMvc.perform(put("/api/v1/lesson-drafts/{draftId}/sources/reorder", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reorderPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources[0].type").value("DOCUMENT_FILE"))
                .andExpect(jsonPath("$.sources[0].position").value(0))
                .andExpect(jsonPath("$.sources[1].type").value("TEXT_NOTE"))
                .andExpect(jsonPath("$.sources[1].position").value(1));

        mockMvc.perform(get("/api/v1/lesson-drafts/{draftId}", draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftId.toString()))
                .andExpect(jsonPath("$.sources.length()").value(2));

        mockMvc.perform(get("/api/v1/lesson-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].sourceCount").value(2));

        mockMvc.perform(delete("/api/v1/lesson-drafts/{draftId}", draftId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/lesson-drafts/{draftId}", draftId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSourceRejectsIncompatiblePayload() throws Exception {
        var createPayload = objectMapper.writeValueAsString(new CreateDraftPayload("Payload test", null, null, null, null));
        var createResponse = mockMvc.perform(post("/api/v1/lesson-drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var draftId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        var invalidPayload = objectMapper.writeValueAsString(
                new AddSourcePayload("TEXT_NOTE", "note", UUID.randomUUID(), "should-not-be-here.pdf"));

        mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    private static UUID findSourceIdByType(JsonNode draftJson, String type) {
        for (var source : draftJson.get("sources")) {
            if (type.equals(source.get("type").asText())) {
                return UUID.fromString(source.get("id").asText());
            }
        }
        throw new IllegalStateException("Source type not found in payload: " + type);
    }

    private UUID uploadTextFile(String originalFileName, String text) throws Exception {
        var bytes = text.getBytes(StandardCharsets.UTF_8);
        var sessionPayload = objectMapper.writeValueAsString(Map.of(
                "scenario",
                "GENERIC_UPLOAD",
                "expectedContentLength",
                bytes.length,
                "declaredContentType",
                "text/plain",
                "originalFileName",
                originalFileName));
        var sessionResponse = mockMvc.perform(post("/api/v1/stored-files/upload-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var sessionId = UUID.fromString(objectMapper.readTree(sessionResponse).get("sessionId").asText());

        var uploadResponse = mockMvc.perform(post("/api/v1/stored-files/upload-sessions/{sessionId}/content", sessionId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .header(StoredFileController.HEADER_ORIGINAL_FILE_NAME, originalFileName)
                        .content(bytes))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(uploadResponse).get("id").asText());
    }

    private record CreateDraftPayload(
            String title, String description, String userInstructions, String explanationLanguage, String translationLanguage) {}

    private record AddSourcePayload(
            String type, String textContent, UUID documentFileId, String documentOriginalFileName) {}

    private record ReorderSourcesPayload(List<UUID> sourceIds) {}
}
