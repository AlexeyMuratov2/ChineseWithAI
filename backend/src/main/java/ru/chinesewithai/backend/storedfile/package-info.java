/**
 * Stored file module: physical blob storage (S3-compatible) and technical metadata only.
 *
 * <p>Other modules must depend on {@code storedfile.application} (e.g. {@link
 * ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade}) — not on web or
 * infrastructure types — so bucket/object-key details stay encapsulated.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Stored file",
        allowedDependencies = {})
package ru.chinesewithai.backend.storedfile;
