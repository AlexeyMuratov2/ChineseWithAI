package ru.chinesewithai.backend.user.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.chinesewithai.backend.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void registerReturnsCreatedUser() throws Exception {
        var request = objectMapper.writeValueAsString(new RegisterPayload("Test_User", "StrongPass123!", "Test User"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void registerReturnsConflictForDuplicateUsername() throws Exception {
        var first = objectMapper.writeValueAsString(new RegisterPayload("john_doe", "StrongPass123!", null));
        var second = objectMapper.writeValueAsString(new RegisterPayload("John_Doe", "StrongPass123!", null));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        register("john_doe", "StrongPass123!", "John");
        var login = objectMapper.writeValueAsString(new LoginPayload("JOHN_DOE", "StrongPass123!"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        register("john_doe", "StrongPass123!", "John");
        var login = objectMapper.writeValueAsString(new LoginPayload("john_doe", "wrongpass123"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsForbiddenForDisabledAccount() throws Exception {
        register("disabled_user", "StrongPass123!", "Disabled");
        jdbcTemplate.update("UPDATE app_user SET status = 'DISABLED' WHERE username = ?", "disabled_user");

        var login = objectMapper.writeValueAsString(new LoginPayload("disabled_user", "StrongPass123!"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isForbidden());
    }

    private void register(String username, String password, String displayName) throws Exception {
        var payload = objectMapper.writeValueAsString(new RegisterPayload(username, password, displayName));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
    }

    private record RegisterPayload(String username, String password, String displayName) {}

    private record LoginPayload(String username, String password) {}
}
