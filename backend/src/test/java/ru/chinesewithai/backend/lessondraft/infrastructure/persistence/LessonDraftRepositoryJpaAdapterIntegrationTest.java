package ru.chinesewithai.backend.lessondraft.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.chinesewithai.backend.TestcontainersConfiguration;
import ru.chinesewithai.backend.lessondraft.application.port.out.LessonDraftRepository;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraft;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class LessonDraftRepositoryJpaAdapterIntegrationTest {

    @Autowired
    private LessonDraftRepository lessonDraftRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM lesson_draft_sources");
        jdbcTemplate.update("DELETE FROM lesson_drafts");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void saveAndFindByIdAndOwnerLoadsSources() {
        var ownerId = insertUser("repo_user_1");
        var now = Instant.now();
        var draft = LessonDraft.createNew(ownerId, "Draft One", "desc", "instructions", null, null, now)
                .addTextSource("note", now.plusSeconds(1))
                .addDocumentSource(UUID.randomUUID(), "book.pdf", now.plusSeconds(2));

        var saved = lessonDraftRepository.save(draft);
        var loaded = lessonDraftRepository.findByIdAndOwnerId(saved.id(), ownerId).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.ownerId()).isEqualTo(ownerId);
        assertThat(loaded.sourceCount()).isEqualTo(2);
        assertThat(loaded.sources().get(0).position()).isEqualTo(0);
        assertThat(loaded.sources().get(1).position()).isEqualTo(1);
    }

    @Test
    void deleteRemovesChildSourcesByCascade() {
        var ownerId = insertUser("repo_user_2");
        var now = Instant.now();
        var draft = LessonDraft.createNew(ownerId, "Draft Two", null, null, null, null, now)
                .addTextSource("text note", now.plusSeconds(1));
        var saved = lessonDraftRepository.save(draft);

        lessonDraftRepository.delete(saved);

        var sourceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_draft_sources WHERE draft_id = ?",
                Integer.class,
                saved.id().value());
        assertThat(sourceCount).isZero();
    }

    @Test
    void findPageByOwnerReturnsDraftsWithCounts() {
        var ownerId = insertUser("repo_user_3");
        var now = Instant.now();
        var older = LessonDraft.createNew(ownerId, "Older", null, null, null, null, now).addTextSource("old", now.plusSeconds(1));
        var newer = LessonDraft.createNew(ownerId, "Newer", null, null, null, null, now.plusSeconds(10))
                .addTextSource("new-1", now.plusSeconds(11))
                .addTextSource("new-2", now.plusSeconds(12));

        var olderSaved = lessonDraftRepository.save(older);
        var newerSaved = lessonDraftRepository.save(newer);

        var page = lessonDraftRepository.findPageByOwnerId(ownerId, 0, 10);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).id()).isEqualTo(newerSaved.id().value());
        assertThat(page.items().get(0).sourceCount()).isEqualTo(2);
        assertThat(page.items().get(1).id()).isEqualTo(olderSaved.id().value());
        assertThat(page.items().get(1).sourceCount()).isEqualTo(1);
    }

    private UUID insertUser(String username) {
        var id = UUID.randomUUID();
        var now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO app_user (id, username, password_hash, display_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)
                """,
                id,
                username,
                "hashed-password",
                username,
                Timestamp.from(now),
                Timestamp.from(now));
        return id;
    }
}
