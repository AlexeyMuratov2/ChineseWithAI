package ru.chinesewithai.backend.storedfile.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

class S3ClientConfigurationTest {

    @Test
    void storedFileBucketEnsurer_skipsWhenAutoCreateDisabled() throws Exception {
        var config = new S3ClientConfiguration();
        S3Client s3 = mock(S3Client.class);
        var props =
                new StoredFileS3Properties(
                        "http://127.0.0.1:9000", "us-east-1", "b", "k", "s", false);
        var runner = config.storedFileBucketEnsurer(s3, props);
        runner.run(mock(ApplicationArguments.class));
        org.mockito.Mockito.verifyNoInteractions(s3);
    }

    @Test
    void storedFileBucketEnsurer_swallowsSdkClientExceptionWhenConnectionRefused() throws Exception {
        var config = new S3ClientConfiguration();
        S3Client s3 = mock(S3Client.class);
        var props =
                new StoredFileS3Properties(
                        "http://127.0.0.1:9000", "us-east-1", "b", "k", "s", true);
        var ex =
                SdkClientException.builder()
                        .message("Unable to execute HTTP request: Connection refused")
                        .cause(new ConnectException("Connection refused"))
                        .build();
        doThrow(ex).when(s3).createBucket(any(CreateBucketRequest.class));
        var runner = config.storedFileBucketEnsurer(s3, props);
        runner.run(mock(ApplicationArguments.class));
        verify(s3).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void storedFileBucketEnsurer_propagatesSdkClientExceptionWhenNotConnectivity() {
        var config = new S3ClientConfiguration();
        S3Client s3 = mock(S3Client.class);
        var props =
                new StoredFileS3Properties(
                        "http://127.0.0.1:9000", "us-east-1", "b", "k", "s", true);
        var ex = SdkClientException.builder().message("Unexpected client failure").build();
        doThrow(ex).when(s3).createBucket(any(CreateBucketRequest.class));
        var runner = config.storedFileBucketEnsurer(s3, props);
        assertThatThrownBy(() -> runner.run(mock(ApplicationArguments.class))).isSameAs(ex);
    }

    @Test
    void storedFileBucketEnsurer_headBucketWhenCreateReportsBucketExists() throws Exception {
        var config = new S3ClientConfiguration();
        S3Client s3 = mock(S3Client.class);
        var props =
                new StoredFileS3Properties(
                        "http://127.0.0.1:9000", "us-east-1", "my-bucket", "k", "s", true);
        var createConflict = software.amazon.awssdk.services.s3.model.S3Exception.builder()
                .message("BucketAlreadyOwnedByYou")
                .statusCode(409)
                .build();
        doThrow(createConflict).when(s3).createBucket(any(CreateBucketRequest.class));
        when(s3.headBucket(any(HeadBucketRequest.class))).thenReturn(null);
        var runner = config.storedFileBucketEnsurer(s3, props);
        runner.run(mock(ApplicationArguments.class));
        verify(s3).createBucket(any(CreateBucketRequest.class));
        verify(s3).headBucket(any(HeadBucketRequest.class));
    }
}
