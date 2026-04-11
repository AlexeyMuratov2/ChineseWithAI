package ru.chinesewithai.backend.lessondraft.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
class LessonDraftControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM lesson_draft_sources");
        jdbcTemplate.update("DELETE FROM lesson_drafts");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/lesson-drafts")).andExpect(status().isUnauthorized());
    }

    @Test
    void fullFlowSupportsOwnerScopedCrudAndSources() throws Exception {
        register("owner_user", "StrongPass123!", "Owner User");
        var ownerToken = login("owner_user", "StrongPass123!");

        var createPayload = objectMapper.writeValueAsString(new CreateDraftPayload("HSK 3 travel", "trip prep", "focus on dialogs", null, null));
        var createResponse = mockMvc.perform(post("/api/v1/lesson-drafts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addTextPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var textSourceId = UUID.fromString(objectMapper.readTree(afterText).get("sources").get(0).get("id").asText());

        var documentFileId = UUID.randomUUID();
        var addDocPayload = objectMapper.writeValueAsString(
                new AddSourcePayload("DOCUMENT_FILE", null, documentFileId, "airport-handbook.pdf"));
        var afterDoc = mockMvc.perform(post("/api/v1/lesson-drafts/{draftId}/sources", draftId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addDocPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var afterDocJson = objectMapper.readTree(afterDoc);
        var docSourceId = findSourceIdByType(afterDocJson, "DOCUMENT_FILE");

        var reorderPayload = objectMapper.writeValueAsString(new ReorderSourcesPayload(List.of(docSourceId, textSourceId)));
        mockMvc.perform(put("/api/v1/lesson-drafts/{draftId}/sources/reorder", draftId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reorderPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources[0].type").value("DOCUMENT_FILE"))
                .andExpect(jsonPath("$.sources[0].position").value(0))
                .andExpect(jsonPath("$.sources[1].type").value("TEXT_NOTE"))
                .andExpect(jsonPath("$.sources[1].position").value(1));

        mockMvc.perform(get("/api/v1/lesson-drafts/{draftId}", draftId).header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftId.toString()))
                .andExpect(jsonPath("$.sources.length()").value(2));

        mockMvc.perform(get("/api/v1/lesson-drafts").header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].sourceCount").value(2));

        register("other_user", "StrongPass123!", "Other User");
        var otherToken = login("other_user", "StrongPass123!");
        mockMvc.perform(get("/api/v1/lesson-drafts/{draftId}", draftId).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/lesson-drafts/{draftId}", draftId).header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/lesson-drafts/{draftId}", draftId).header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSourceRejectsIncompatiblePayload() throws Exception {
        register("payload_user", "StrongPass123!", "Payload User");
        var token = login("payload_user", "StrongPass123!");

        var createPayload = objectMapper.writeValueAsString(new CreateDraftPayload("Payload test", null, null, null, null));
        var createResponse = mockMvc.perform(post("/api/v1/lesson-drafts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
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

    private static UUID findSourceIdByType(JsonNode draftJson, String type) {
        for (var source : draftJson.get("sources")) {
            if (type.equals(source.get("type").asText())) {
                return UUID.fromString(source.get("id").asText());
            }
        }
        throw new IllegalStateException("Source type not found in payload: " + type);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record RegisterPayload(String username, String password, String displayName) {}

    private record LoginPayload(String username, String password) {}

    private record CreateDraftPayload(
            String title, String description, String userInstructions, String explanationLanguage, String translationLanguage) {}

    private record AddSourcePayload(
            String type, String textContent, UUID documentFileId, String documentOriginalFileName) {}

    private record ReorderSourcesPayload(List<UUID> sourceIds) {}
}
