package ru.chinesewithai.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Ensures MinIO-backed S3 properties are registered before the context starts. Keeps Docker-based
 * object storage consistent for every {@link SpringBootTest} without relying on
 * {@code ApplicationContextInitializer} auto-registration (which varies by Boot version).
 */
@SpringBootTest
@ContextConfiguration(initializers = MinioTestInitializer.class)
public abstract class AbstractIntegrationTest {}
