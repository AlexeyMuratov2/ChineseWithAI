package ru.chinesewithai.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
class ModularityVerificationTest {

    @Test
    void verifiesApplicationModules() {
        ApplicationModules.of(BackendApplication.class).verify();
    }
}
