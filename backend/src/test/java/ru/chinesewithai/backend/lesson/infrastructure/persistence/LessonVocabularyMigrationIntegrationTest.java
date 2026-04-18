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
class LessonVocabularyMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void v10CreatesVocabularyTablesAndSeedsLessonGeneratorWorkflow() throws Exception {
        var schema = "lesson_v10_schema_test";
        recreateSchema(schema);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("9")
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
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = '"
                            + schema
                            + "' AND table_name IN ('lesson_vocabulary_items', 'learner_vocabulary_progress') ORDER BY table_name")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("table_name")).isEqualTo("learner_vocabulary_progress");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("table_name")).isEqualTo("lesson_vocabulary_items");
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT conname FROM pg_constraint c "
                            + "JOIN pg_namespace n ON n.oid = c.connamespace "
                            + "WHERE n.nspname = '"
                            + schema
                            + "' AND conname IN ("
                            + "'fk_lesson_vocabulary_items_lesson_id',"
                            + "'fk_lesson_vocabulary_items_user_id',"
                            + "'chk_learner_vocabulary_progress_status'"
                            + ") ORDER BY conname")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("conname")).isEqualTo("chk_learner_vocabulary_progress_status");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("conname")).isEqualTo("fk_lesson_vocabulary_items_lesson_id");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("conname")).isEqualTo("fk_lesson_vocabulary_items_user_id");
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT indexname FROM pg_indexes WHERE schemaname = '"
                            + schema
                            + "' AND indexname IN ("
                            + "'uq_lesson_vocabulary_items_lesson_word',"
                            + "'uq_learner_vocabulary_progress_user_word'"
                            + ") ORDER BY indexname")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("indexname")).isEqualTo("uq_learner_vocabulary_progress_user_word");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("indexname")).isEqualTo("uq_lesson_vocabulary_items_lesson_word");
            }

            try (ResultSet rs = statement.executeQuery(
                    "SELECT workflow_variant_key, steps_json FROM "
                            + schema
                            + ".agent_pre_generation_workflows "
                            + "WHERE profile_key = 'lesson-generator:v1' "
                            + "AND workflow_variant_key = 'draft-generation-with-review:v1'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("workflow_variant_key")).isEqualTo("draft-generation-with-review:v1");
                assertThat(rs.getString("steps_json")).contains("lesson-vocabulary-review-plan");
            }
        }
    }

    @Test
    void v11AddsReviewWordsToLessonGeneratorContract() throws Exception {
        var schema = "lesson_v11_schema_test";
        recreateSchema(schema);

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("10")
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
                    "SELECT output_contract_json, system_prompt FROM "
                            + schema
                            + ".agent_profiles WHERE profile_key = 'lesson-generator:v1'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("output_contract_json")).contains("\"reviewWords\":\"array\"");
                assertThat(rs.getString("system_prompt")).contains("reviewWords");
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
