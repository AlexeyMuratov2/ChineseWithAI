package ru.chinesewithai.backend;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a single MinIO container for the whole JVM and wires S3-compatible properties before the
 * Spring context refreshes. Registered via {@code META-INF/spring/...ApplicationContextInitializer.imports}
 * so every {@link org.springframework.boot.test.context.SpringBootTest} gets a working object store
 * without duplicating container setup per test class.
 */
public class MinioTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final GenericContainer<?> MINIO =
            new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                    .withExposedPorts(9000)
                    .withEnv("MINIO_ROOT_USER", "testaccess")
                    .withEnv("MINIO_ROOT_PASSWORD", "testsecretkey")
                    .withCommand("server", "/data");

    static {
        MINIO.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var host = MINIO.getHost();
        var port = MINIO.getMappedPort(9000);
        TestPropertyValues.of(
                        "app.stored-file.s3.endpoint=http://" + host + ":" + port,
                        "app.stored-file.s3.region=us-east-1",
                        "app.stored-file.s3.bucket=test-bucket",
                        "app.stored-file.s3.access-key=testaccess",
                        "app.stored-file.s3.secret-key=testsecretkey",
                        "app.stored-file.s3.auto-create-bucket=true")
                .applyTo(context.getEnvironment());
    }
}
