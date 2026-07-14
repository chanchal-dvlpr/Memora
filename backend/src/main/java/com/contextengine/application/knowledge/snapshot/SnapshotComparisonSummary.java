package com.contextengine.application.knowledge.snapshot;

/**
 * Summary details of matching difference counts.
 */
public record SnapshotComparisonSummary(
    int added,
    int removed,
    int modified,
    int renamed,
    int moved,
    int unchanged
) {}
