package ru.chinesewithai.backend.storedfile.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
class StoredFileControllerIntegrationTest extends AbstractIntegrationTest {

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
    void sessionUploadDownloadDeleteAndIdempotentDelete() throws Exception {
        var sessionPayload = """
                {"scenario":"GENERIC_UPLOAD","expectedContentLength":11}
                """;
        var sessionResponse = mockMvc.perform(post("/api/v1/stored-files/upload-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var sessionId = UUID.fromString(objectMapper.readTree(sessionResponse).get("sessionId").asText());

        var bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        var uploadResponse = mockMvc.perform(post("/api/v1/stored-files/upload-sessions/{sessionId}/content", sessionId)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(bytes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sizeBytes").value(11))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var fileId = UUID.fromString(objectMapper.readTree(uploadResponse).get("id").asText());

        mockMvc.perform(get("/api/v1/stored-files/upload-sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.resultFileId").value(fileId.toString()));

        mockMvc.perform(get("/api/v1/stored-files/{fileId}/content", fileId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes));

        mockMvc.perform(delete("/api/v1/stored-files/{fileId}", fileId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/stored-files/{fileId}", fileId))
                .andExpect(status().isNoContent());
    }
}
