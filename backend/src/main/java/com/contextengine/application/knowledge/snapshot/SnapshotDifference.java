package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.valueobject.EngineeringEvidence;

/**
 * Record holding comparison differences between previous and current snapshots.
 */
public record SnapshotDifference(
    SnapshotDifferenceType differenceType,
    EngineeringEvidence previousEvidence,
    EngineeringEvidence currentEvidence,
    String description
) {
    public SnapshotDifference {
        java.util.Objects.requireNonNull(differenceType, "differenceType must not be null");
        java.util.Objects.requireNonNull(description, "description must not be null");
    }
}
