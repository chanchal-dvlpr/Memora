package com.contextengine.application.scanner.hashing;

import java.time.Instant;
import java.util.Objects;

/**
 * Encapsulates the results of running a bottom-up Merkle structural hashing run.
 */
public class StructuralHashResult {
    private final String workspaceHash;
    private final long totalNodes;
    private final long directoryCount;
    private final long fileCount;
    private final String hashAlgorithm;
    private final Instant generatedAt;

    /**
     * Constructs a StructuralHashResult.
     *
     * @param workspaceHash deterministic top-level project root hash
     * @param totalNodes total count of processed nodes (files + directories)
     * @param directoryCount count of directory nodes
     * @param fileCount count of file nodes
     * @param hashAlgorithm hashing algorithm ("SHA-256")
     * @param generatedAt creation timestamp
     */
    public StructuralHashResult(String workspaceHash, long totalNodes, long directoryCount, long fileCount, String hashAlgorithm, Instant generatedAt) {
        this.workspaceHash = Objects.requireNonNull(workspaceHash, "WorkspaceHash must not be null");
        this.totalNodes = totalNodes;
        this.directoryCount = directoryCount;
        this.fileCount = fileCount;
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm, "HashAlgorithm must not be null");
        this.generatedAt = Objects.requireNonNull(generatedAt, "GeneratedAt must not be null");
    }

    public String workspaceHash() {
        return workspaceHash;
    }

    public long totalNodes() {
        return totalNodes;
    }

    public long directoryCount() {
        return directoryCount;
    }

    public long fileCount() {
        return fileCount;
    }

    public String hashAlgorithm() {
        return hashAlgorithm;
    }

    public Instant generatedAt() {
        return generatedAt;
    }
}
