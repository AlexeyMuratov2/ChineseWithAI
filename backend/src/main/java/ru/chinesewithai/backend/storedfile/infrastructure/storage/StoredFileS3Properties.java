package ru.chinesewithai.backend.storedfile.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized S3-compatible settings (MinIO in local docker, real S3 in production). Wired only from
 * configuration — never hard-coded bucket names in application services.
 */
@ConfigurationProperties(prefix = "app.stored-file.s3")
public record StoredFileS3Properties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean autoCreateBucket) {

    public StoredFileS3Properties {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
    }
}
