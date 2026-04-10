package ru.chinesewithai.backend;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
