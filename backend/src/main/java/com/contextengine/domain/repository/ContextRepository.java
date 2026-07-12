package com.contextengine.domain.repository;

import com.contextengine.domain.entity.Context;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.DateRange;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import java.util.Collection;
import java.util.Optional;

/**
 * Governs the retention and historical tracking of the Context Aggregate Root.
 */
public interface ContextRepository {
    
    /**
     * Stores compiled contexts, active snapshot sequences, and historical changes.
     *
     * @param context the context aggregate root
     */
    void save(Context context);
    
    /**
     * Resolves an immutable compiled context snapshot block.
     *
     * @param snapshotId the snapshot ID
     * @return an optional containing the snapshot, or empty if not found
     */
    Optional<ContextSnapshot> findSnapshotById(SnapshotId snapshotId);
    
    /**
     * Extracts the most recently generated context snapshot.
     *
     * @param projectId the associated project ID
     * @return an optional containing the latest snapshot, or empty if not found
     */
    Optional<ContextSnapshot> findLatestSnapshotForProject(ProjectId projectId);
    
    /**
     * Returns chronological sequences of snapshot captures within a time range.
     *
     * @param projectId the associated project ID
     * @param range the date range window parameters
     * @return collection of historical snapshots
     */
    Collection<ContextSnapshot> findHistoryByDateRange(ProjectId projectId, DateRange range);
    
    /**
     * Deletes historic context state frames preceding the cutoff timestamp.
     *
     * @param projectId the associated project ID
     * @param retentionCutoff the expiration cutoff timestamp
     * @return the number of pruned snapshots
     */
    int pruneOldSnapshots(ProjectId projectId, Timestamp retentionCutoff);
    
    /**
     * Removes an immutable context snapshot.
     *
     * @param snapshotId the snapshot ID to delete
     */
    void removeSnapshot(SnapshotId snapshotId);
}
