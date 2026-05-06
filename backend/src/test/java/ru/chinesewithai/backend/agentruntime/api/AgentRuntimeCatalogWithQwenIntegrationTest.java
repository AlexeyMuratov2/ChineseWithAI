package ru.chinesewithai.backend.agentruntime.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.chinesewithai.backend.AbstractIntegrationTest;
import ru.chinesewithai.backend.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.agentruntime.qwen.api-key=test-qwen-key")
class AgentRuntimeCatalogWithQwenIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void modelsCatalogExposesQwenWhenApiKeyIsConfigured() throws Exception {
        mockMvc.perform(get("/api/v1/agent-runtime/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].modelKey", containsInAnyOrder("deepseek-chat", "qwen3.6-plus")))
                .andExpect(jsonPath("$[?(@.modelKey == 'qwen3.6-plus')].providerKey", hasItem("qwen")))
                .andExpect(jsonPath("$[?(@.modelKey == 'qwen3.6-plus')].displayName", hasItem("Qwen3.6 Plus")))
                .andExpect(jsonPath("$[?(@.modelKey == 'qwen3.6-plus')].capabilities[*]", hasItem("image_input")));
    }
}
