package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.entity.ContextSnapshot;
import java.util.Objects;

/**
 * Pipeline coordinating input parameters to ComparisonEngine calculations.
 */
public class SnapshotComparisonPipeline {
    private final SnapshotComparisonEngine engine;

    public SnapshotComparisonPipeline(SnapshotComparisonEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    public SnapshotComparisonResult execute(
        ContextSnapshot previousSnapshot,
        ContextSnapshot currentSnapshot,
        SnapshotComparisonConfiguration configuration
    ) {
        return execute(previousSnapshot, currentSnapshot, configuration, "", false);
    }

    public SnapshotComparisonResult execute(
        ContextSnapshot previousSnapshot,
        ContextSnapshot currentSnapshot,
        SnapshotComparisonConfiguration configuration,
        String structuralHash,
        boolean isIncremental
    ) {
        SnapshotComparisonContext context = new SnapshotComparisonContext(
            previousSnapshot,
            currentSnapshot,
            configuration,
            structuralHash,
            isIncremental
        );
        return engine.compare(context);
    }
}
