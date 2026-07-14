package com.contextengine.application.knowledge.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulated output result carrying differences and summary metrics.
 */
public record SnapshotComparisonResult(
    String projectId,
    List<SnapshotDifference> differences,
    SnapshotComparisonSummary summary,
    SnapshotComparisonStatistics statistics,
    Instant timestamp
) {
    public SnapshotComparisonResult {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(differences, "differences must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(statistics, "statistics must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
