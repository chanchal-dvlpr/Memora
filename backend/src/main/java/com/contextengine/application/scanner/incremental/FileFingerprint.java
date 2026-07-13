package com.contextengine.application.scanner.incremental;

import java.util.Objects;

/**
 * Represents a cached file fingerprint (relative path, size, and last modified timestamp)
 * used to detect modifications between scan runs.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public record FileFingerprint(
    String relativePath,
    long size,
    long lastModified
) {
    /**
     * Constructs a FileFingerprint.
     *
     * @param relativePath file path relative to project root
     * @param size file size in bytes
     * @param lastModified file last modified epoch milliseconds
     */
    public FileFingerprint {
        Objects.requireNonNull(relativePath, "RelativePath must not be null");
    }
}
