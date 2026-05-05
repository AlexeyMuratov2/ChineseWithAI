package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.chinesewithai.backend.AbstractIntegrationTest;
import ru.chinesewithai.backend.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AgentRuntimeMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void latestMigrationBackfillsLegacySessionModelTaskAndAddsOutputRepairSettings() throws Exception {
        var schema = "agentruntime_v7_backfill_test";
        recreateSchema(schema);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("5")
                .load()
                .migrate();

        var userId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO " + schema + ".app_user (id, username, password_hash, display_name, status, created_at, updated_at) "
                    + "VALUES ('" + userId + "', 'legacy_user', 'hash', 'Legacy User', 'ACTIVE', NOW(), NOW())");
            statement.executeUpdate("INSERT INTO " + schema + ".agent_sessions (id, owner_id, profile_key, status, input_json, final_output_json, failure_reason, created_at, started_at, finished_at, updated_at) "
                    + "VALUES ('" + sessionId + "', '" + userId + "', 'test-agent:v1', 'CREATED', '{\"objective\":\"legacy\"}', NULL, NULL, NOW(), NULL, NULL, NOW())");
        }

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet sessionRs = statement.executeQuery("SELECT model_key, task, input_json, system_prompt_appendix FROM "
                        + schema + ".agent_sessions WHERE id = '" + sessionId + "'")) {
            assertThat(sessionRs.next()).isTrue();
            assertThat(sessionRs.getString("model_key")).isEqualTo("fake-model");
            assertThat(sessionRs.getString("task")).isEqualTo("{\"objective\":\"legacy\"}");
            assertThat(sessionRs.getString("input_json")).isEqualTo("{\"objective\":\"legacy\"}");
            assertThat(sessionRs.getString("system_prompt_appendix")).isNull();
        }

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet profileRs = statement.executeQuery(
                        "SELECT profile_key, is_visible, auto_repair_invalid_output_enabled, output_validation_strategy_key "
                                + "FROM "
                                + schema
                                + ".agent_profiles ORDER BY profile_key")) {
            var profiles = new LinkedHashMap<String, ProfileRow>();
            while (profileRs.next()) {
                profiles.put(
                        profileRs.getString("profile_key"),
                        new ProfileRow(
                                profileRs.getBoolean("is_visible"),
                                profileRs.getBoolean("auto_repair_invalid_output_enabled"),
                                profileRs.getString("output_validation_strategy_key")));
            }

            assertThat(profiles.keySet())
                    .contains(
                            "assistant:v1",
                            "lesson-generator:hsk5_v1",
                            "lesson-generator:hsk5_v1_composer",
                            "lesson-generator:v1",
                            "lesson-stage:hsk5_v1_blueprint",
                            "lesson-stage:hsk5_v1_grammar",
                            "lesson-stage:hsk5_v1_vocabulary_practice",
                            "lesson-stage:hsk5_v1_word_game",
                            "test-agent:v1");
            assertThat(profiles.get("assistant:v1").visible()).isTrue();
            assertThat(profiles.get("assistant:v1").autoRepair()).isFalse();
            assertThat(profiles.get("lesson-generator:hsk5_v1_composer").visible()).isFalse();
            assertThat(profiles.get("lesson-generator:hsk5_v1_composer").autoRepair()).isTrue();
            assertThat(profiles.get("lesson-generator:hsk5_v1_composer").strategyKey())
                    .isEqualTo("lesson-generated-content");
            assertThat(profiles.get("lesson-stage:hsk5_v1_blueprint").autoRepair()).isFalse();
        }

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO " + schema + ".agent_steps (id, session_id, step_index, step_type, payload_json, created_at) "
                    + "VALUES ('" + UUID.randomUUID() + "', '" + sessionId + "', 0, 'OUTPUT_VALIDATION_FAILED', '{}', NOW())");
        }
    }

    private record ProfileRow(boolean visible, boolean autoRepair, String strategyKey) {}

    private void recreateSchema(String schema) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            statement.execute("CREATE SCHEMA " + schema);
        }
    }
}
