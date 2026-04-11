package ru.chinesewithai.backend.storedfile.application.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Wraps an upload stream to count bytes and invoke a callback for progress. Used by upload
 * orchestration so MinIO/S3 consumption and progress reporting share a single read pass.
 */
public final class CountingProgressInputStream extends FilterInputStream {

    private final Optional<Long> expectedTotal;
    private final BiConsumer<Long, Optional<Integer>> onProgress;
    private final long notifyStrideBytes;
    private long totalRead;
    private long sinceLastNotify;

    public CountingProgressInputStream(
            InputStream delegate,
            Optional<Long> expectedTotal,
            long notifyStrideBytes,
            BiConsumer<Long, Optional<Integer>> onProgress) {
        super(delegate);
        this.expectedTotal = expectedTotal;
        this.notifyStrideBytes = Math.max(1, notifyStrideBytes);
        this.onProgress = onProgress;
    }

    @Override
    public int read() throws IOException {
        int r = super.read();
        if (r >= 0) {
            recordBytes(1);
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int r = super.read(b, off, len);
        if (r > 0) {
            recordBytes(r);
        }
        return r;
    }

    private void recordBytes(int n) {
        totalRead += n;
        sinceLastNotify += n;
        if (sinceLastNotify >= notifyStrideBytes) {
            sinceLastNotify = 0;
            fireProgress();
        }
    }

    private void fireProgress() {
        Optional<Integer> percent =
                expectedTotal.filter(t -> t > 0).map(t -> (int) Math.min(100, (totalRead * 100) / t));
        onProgress.accept(totalRead, percent);
    }

    /** Call after EOF so final percent reaches 100 when Content-Length was known. */
    public void finishProgress() {
        fireProgress();
    }

    public long getTotalBytesRead() {
        return totalRead;
    }
}
