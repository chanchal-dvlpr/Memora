package com.contextengine.application.scanner;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable metadata record representing a physical file candidate discovered during scanning.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public record ScanCandidate(
    String relativePath,
    String absolutePath,
    long size,
    Instant lastModified,
    String nodeType, // FILE or DIR
    SupportedLanguage language
) {

    /**
     * Constructs a ScanCandidate.
     *
     * @param relativePath relative file path
     * @param absolutePath absolute file path on host workstation
     * @param size size of file in bytes
     * @param lastModified timestamp of last modification
     * @param nodeType node classification (FILE or DIR)
     * @param language detected programming language
     */
    public ScanCandidate {
        Objects.requireNonNull(relativePath, "RelativePath must not be null");
        Objects.requireNonNull(absolutePath, "AbsolutePath must not be null");
        Objects.requireNonNull(lastModified, "LastModified must not be null");
        Objects.requireNonNull(nodeType, "NodeType must not be null");
        Objects.requireNonNull(language, "Language must not be null");

        if (size < 0) {
            throw new IllegalArgumentException("File size must be non-negative");
        }
    }
}
