package ru.chinesewithai.backend.learnerprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
class LearnerProfileContextMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void v14CreatesLearnerContextsSeedsHsk5AndUpdatesDefaultWorkflow() throws Exception {
        var schema = "learner_profile_v14_schema_test";
        recreateSchema(schema);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("13")
                .load()
                .migrate();

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "SELECT data_type, udt_name FROM information_schema.columns WHERE table_schema = '"
                            + schema
                            + "' AND table_name = 'learner_profile_contexts' AND column_name = 'content_json'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("data_type")).isEqualTo("jsonb");
                assertThat(rs.getString("udt_name")).isEqualTo("jsonb");
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT content_json::text, is_active FROM "
                            + schema
                            + ".learner_profile_contexts WHERE profile_key = 'lesson-generator:hsk5_v1'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("content_json")).contains("Ученик уровня HSK5上");
                assertThat(rs.getBoolean("is_active")).isTrue();
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT steps_json FROM "
                            + schema
                            + ".agent_pre_generation_workflows "
                            + "WHERE profile_key = 'lesson-generator:hsk5_v1' AND workflow_variant_key IS NULL")) {
                assertThat(rs.next()).isTrue();
                var stepsJson = rs.getString("steps_json");
                assertThat(stepsJson).doesNotContain("current-user-profile");
                assertThat(stepsJson).contains("learner-profile-context");
                assertThat(stepsJson).contains("lesson-vocabulary-review-plan");
                assertThat(stepsJson.indexOf("learner-profile-context"))
                        .isLessThan(stepsJson.indexOf("lesson-vocabulary-review-plan"));
            }
        }
    }

    private void recreateSchema(String schema) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            statement.execute("CREATE SCHEMA " + schema);
        }
    }
}
