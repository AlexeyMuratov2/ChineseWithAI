package ru.chinesewithai.backend.storedfile.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultFileUploadSecurityStrategyFactoryTest {

    @Test
    void resolvesSamePermissiveBeanForEveryScenario() {
        var permissive = new PermissiveFileUploadSecurityStrategy();
        var factory = new DefaultFileUploadSecurityStrategyFactory(permissive);

        assertThat(factory.forScenario(UploadScenario.GENERIC_UPLOAD)).isSameAs(permissive);
        assertThat(factory.forScenario(UploadScenario.CROSS_MODULE_STREAM)).isSameAs(permissive);
    }
}
