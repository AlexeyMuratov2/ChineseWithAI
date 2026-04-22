package ru.chinesewithai.backend.teacherpersonality.infrastructure.persistence;

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
class TeacherPersonalityContextMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void v15CreatesTeacherPersonalityContextsSeedsHsk5AndUpdatesDefaultWorkflow() throws Exception {
        var schema = "teacher_personality_v15_schema_test";
        recreateSchema(schema);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("14")
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
                            + "' AND table_name = 'teacher_personality_contexts' AND column_name = 'content_json'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("data_type")).isEqualTo("jsonb");
                assertThat(rs.getString("udt_name")).isEqualTo("jsonb");
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT content_json #>> '{teacherCharacter}' AS teacher_character, is_active FROM "
                            + schema
                            + ".teacher_personality_contexts WHERE profile_key = 'lesson-generator:hsk5_v1'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("teacher_character"))
                        .isEqualTo("\u0422\u044b \u0432\u0435\u0441\u0435\u043b\u044b\u0439 "
                                + "\u0443\u0447\u0438\u0442\u0435\u043b\u044c, \u043a\u043e\u0442\u043e\u0440\u044b\u0439 "
                                + "\u043c\u043e\u0436\u0435\u0442 \u043f\u043e\u0434\u0431\u043e\u0434\u0440\u0438\u0442\u044c "
                                + "\u0438 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0430\u0442\u044c.");
                assertThat(rs.getBoolean("is_active")).isTrue();
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT steps_json FROM "
                            + schema
                            + ".agent_pre_generation_workflows "
                            + "WHERE profile_key = 'lesson-generator:hsk5_v1' AND workflow_variant_key IS NULL")) {
                assertThat(rs.next()).isTrue();
                var stepsJson = rs.getString("steps_json");
                assertThat(stepsJson).contains("current-user-profile");
                assertThat(stepsJson).contains("learner-profile-context");
                assertThat(stepsJson).contains("teacher-personality-context");
                assertThat(stepsJson).contains("lesson-vocabulary-review-plan");
                assertThat(stepsJson.indexOf("current-user-profile"))
                        .isLessThan(stepsJson.indexOf("learner-profile-context"));
                assertThat(stepsJson.indexOf("learner-profile-context"))
                        .isLessThan(stepsJson.indexOf("teacher-personality-context"));
                assertThat(stepsJson.indexOf("teacher-personality-context"))
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
