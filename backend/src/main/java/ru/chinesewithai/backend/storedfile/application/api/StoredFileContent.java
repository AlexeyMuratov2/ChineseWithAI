package ru.chinesewithai.backend.storedfile.application.api;

import java.io.InputStream;
import java.util.Optional;

/**
 * Stream access to a blob. Callers must {@link #close()} (try-with-resources) so the underlying
 * object-storage response is released.
 */
public final class StoredFileContent implements AutoCloseable {

    private final InputStream inputStream;
    private final long sizeBytes;
    private final Optional<String> contentType;
    private final Optional<String> originalFileName;
    private final Runnable closer;

    public StoredFileContent(
            InputStream inputStream,
            long sizeBytes,
            Optional<String> contentType,
            Optional<String> originalFileName,
            Runnable closer) {
        this.inputStream = inputStream;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.originalFileName = originalFileName;
        this.closer = closer;
    }

    public InputStream inputStream() {
        return inputStream;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public Optional<String> contentType() {
        return contentType;
    }

    public Optional<String> originalFileName() {
        return originalFileName;
    }

    @Override
    public void close() {
        closer.run();
    }
}
