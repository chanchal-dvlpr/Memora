package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.entity.ContextSnapshot;
import java.util.Objects;

/**
 * Parameter block carrying snapshot configurations and incremental hashes.
 */
public class SnapshotComparisonContext {
    private final ContextSnapshot previousSnapshot;
    private final ContextSnapshot currentSnapshot;
    private final SnapshotComparisonConfiguration configuration;
    private final String structuralHash;
    private final boolean isIncremental;

    public SnapshotComparisonContext(
        ContextSnapshot previousSnapshot,
        ContextSnapshot currentSnapshot,
        SnapshotComparisonConfiguration configuration
    ) {
        this(previousSnapshot, currentSnapshot, configuration, "", false);
    }

    public SnapshotComparisonContext(
        ContextSnapshot previousSnapshot,
        ContextSnapshot currentSnapshot,
        SnapshotComparisonConfiguration configuration,
        String structuralHash,
        boolean isIncremental
    ) {
        this.previousSnapshot = previousSnapshot;
        this.currentSnapshot = Objects.requireNonNull(currentSnapshot, "currentSnapshot must not be null");
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.structuralHash = structuralHash != null ? structuralHash : "";
        this.isIncremental = isIncremental;
    }

    public ContextSnapshot previousSnapshot() {
        return previousSnapshot;
    }

    public ContextSnapshot currentSnapshot() {
        return currentSnapshot;
    }

    public SnapshotComparisonConfiguration configuration() {
        return configuration;
    }

    public String structuralHash() {
        return structuralHash;
    }

    public boolean isIncremental() {
        return isIncremental;
    }
}
