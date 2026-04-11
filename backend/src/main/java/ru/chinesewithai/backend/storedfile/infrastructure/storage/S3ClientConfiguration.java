package ru.chinesewithai.backend.storedfile.infrastructure.storage;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Constructs a path-style S3 client suitable for MinIO. Real AWS deployments can keep the same
 * client with a virtual-hosted endpoint if path style is disabled later.
 *
 * <p>SDK 2.30+ adds default payload checksums that many S3-compatible servers reject; we only
 * calculate checksums when the operation requires them so MinIO and similar stores keep working.
 */
@Configuration
@EnableConfigurationProperties(StoredFileS3Properties.class)
public class S3ClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(S3ClientConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "app.stored-file.s3", name = "endpoint")
    S3Client storedFileS3Client(StoredFileS3Properties properties) {
        var credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /**
     * Creates the configured bucket when {@link StoredFileS3Properties#autoCreateBucket()} is true.
     * Evaluated at startup (not via {@code @ConditionalOnProperty} on this bean) so relaxed binding
     * from test {@link org.springframework.boot.test.util.TestPropertyValues} always applies.
     */
    @Bean
    @Order(5)
    @ConditionalOnBean(S3Client.class)
    ApplicationRunner storedFileBucketEnsurer(S3Client s3Client, StoredFileS3Properties properties) {
        return args -> {
            if (!properties.autoCreateBucket()) {
                return;
            }
            var bucket = properties.bucket();
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created S3 bucket: {}", bucket);
            } catch (S3Exception createEx) {
                try {
                    s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                    log.debug("S3 bucket already exists: {}", bucket);
                } catch (S3Exception ignored) {
                    throw createEx;
                }
            } catch (SdkClientException ex) {
                if (isConnectivityFailure(ex)) {
                    log.warn(
                            "Cannot reach S3-compatible storage at {} (bucket '{}'). "
                                    + "Skipping startup bucket ensure. File features need a running store "
                                    + "(e.g. `docker compose up -d minio` from repo root) or set "
                                    + "CWA_S3_AUTO_CREATE_BUCKET=false if you are not using file storage yet. "
                                    + "Cause: {}",
                            properties.endpoint(),
                            bucket,
                            ex.getMessage());
                    return;
                }
                throw ex;
            }
        };
    }

    private static boolean isConnectivityFailure(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof java.net.ConnectException) {
                return true;
            }
            if (c instanceof java.net.UnknownHostException) {
                return true;
            }
        }
        String msg = t.getMessage();
        return msg != null && msg.contains("Connection refused");
    }
}
