package ru.chinesewithai.backend;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ModularityVerificationTest extends AbstractIntegrationTest {

    @Test
    void verifiesApplicationModules() {
        ApplicationModules.of(BackendApplication.class).verify();
    }
}
