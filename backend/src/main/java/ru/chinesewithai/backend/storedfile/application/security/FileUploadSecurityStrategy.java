package ru.chinesewithai.backend.storedfile.application.security;

/**
 * Pluggable validation for uploads. Invoked twice so future rules can reject early (before
 * buffering large bodies) and optionally re-check using observed byte counts.
 *
 * <p>Implementations must stay side-effect free except for throwing {@link
 * ru.chinesewithai.backend.storedfile.application.exception.FileUploadRejectedException}.
 */
public interface FileUploadSecurityStrategy {

    void validateBeforeStream(UploadSecurityContext context);

    void validateAfterStream(UploadSecurityContext context, long actualBytesRead);
}
