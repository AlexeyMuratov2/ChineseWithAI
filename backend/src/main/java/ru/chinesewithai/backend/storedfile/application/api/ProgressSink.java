package ru.chinesewithai.backend.storedfile.application.api;

/**
 * Optional callback while bytes are being read from a client or upstream stream. HTTP uploads use
 * {@link ru.chinesewithai.backend.storedfile.application.port.out.UploadProgressNotifier} instead;
 * this type keeps programmatic {@link StoredFileFacade#store} symmetrical.
 */
@FunctionalInterface
public interface ProgressSink {

    void onProgress(int percent, long bytesReceived, Long bytesExpected);

    static ProgressSink noop() {
        return (p, b, t) -> {};
    }
}
