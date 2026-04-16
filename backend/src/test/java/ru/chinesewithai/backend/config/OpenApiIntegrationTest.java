package ru.chinesewithai.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.chinesewithai.backend.AbstractIntegrationTest;
import ru.chinesewithai.backend.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void apiDocsExposeBearerSchemeAndApplyItOnlyToProtectedOperations() throws Exception {
        var response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);

        JsonNode bearerScheme = root.path("components").path("securitySchemes").path(OpenApiConfig.BEARER_AUTH_SCHEME);
        assertThat(bearerScheme.path("type").asText()).isEqualTo("http");
        assertThat(bearerScheme.path("scheme").asText()).isEqualTo("bearer");
        assertThat(bearerScheme.path("bearerFormat").asText()).isEqualTo("JWT");
        assertThat(bearerScheme.path("description").asText()).contains("without the Bearer prefix");

        JsonNode currentUserSecurity =
                root.path("paths").path("/api/v1/users/me").path("get").path("security");
        assertThat(currentUserSecurity.isArray()).isTrue();
        assertThat(currentUserSecurity).hasSize(1);
        assertThat(currentUserSecurity.get(0).has(OpenApiConfig.BEARER_AUTH_SCHEME)).isTrue();

        JsonNode registerSecurity =
                root.path("paths").path("/api/v1/auth/register").path("post").path("security");
        JsonNode loginSecurity =
                root.path("paths").path("/api/v1/auth/login").path("post").path("security");
        assertThat(registerSecurity.isMissingNode()).isTrue();
        assertThat(loginSecurity.isMissingNode()).isTrue();
    }
}
