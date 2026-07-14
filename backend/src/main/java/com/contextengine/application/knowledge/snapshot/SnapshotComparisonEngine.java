package com.contextengine.application.knowledge.snapshot;

/**
 * Port contract for comparing context snapshots.
 */
public interface SnapshotComparisonEngine {

    /**
     * Executes comparison of snapshots.
     *
     * @param context the parameter context
     * @return comparison result
     */
    SnapshotComparisonResult compare(SnapshotComparisonContext context);
}
