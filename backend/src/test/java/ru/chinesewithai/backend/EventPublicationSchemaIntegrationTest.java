package ru.chinesewithai.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class EventPublicationSchemaIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Test
	void flywayCreatesEventPublicationTable() throws Exception {
		try (Connection c = dataSource.getConnection();
				Statement st = c.createStatement();
				ResultSet rs = st.executeQuery("SELECT to_regclass('public.event_publication')")) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString(1)).isEqualTo("event_publication");
		}
	}
}
