package ru.chinesewithai.backend.lesson.infrastructure.persistence;

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
class LessonMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void v7CreatesLessonTablesSeedsTestModuleAndLessonGeneratorProfile() throws Exception {
        var schema = "lesson_v7_schema_test";
        recreateSchema(schema);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("6")
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
                    "SELECT module_key, schema_version, is_active FROM " + schema + ".lesson_modules WHERE module_key = 'TestModule'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("module_key")).isEqualTo("TestModule");
                assertThat(rs.getInt("schema_version")).isEqualTo(1);
                assertThat(rs.getBoolean("is_active")).isTrue();
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT profile_key, is_visible FROM " + schema + ".agent_profiles WHERE profile_key = 'lesson-generator:v1'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean("is_visible")).isFalse();
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = '"
                            + schema
                            + "' AND table_name = 'agent_sessions' AND column_name = 'system_prompt_appendix'")) {
                assertThat(rs.next()).isTrue();
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = '"
                            + schema
                            + "' AND table_name = 'lessons' AND column_name = 'content_json'")) {
                assertThat(rs.next()).isTrue();
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
