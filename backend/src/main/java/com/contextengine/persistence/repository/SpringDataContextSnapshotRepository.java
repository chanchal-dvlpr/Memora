package com.contextengine.persistence.repository;

import com.contextengine.persistence.entity.ContextSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for ContextSnapshotEntity persistence operations.
 * <p>
 * Bounded Context: Context Assembly
 * Related Subsystem: Persistent Storage
 * </p>
 */
public interface SpringDataContextSnapshotRepository extends JpaRepository<ContextSnapshotEntity, String> {

    /**
     * Resolves the latest snapshot entity for a project.
     *
     * @param projectId the associated project ID
     * @return optional containing the latest ContextSnapshotEntity, or empty
     */
    @Query("SELECT s FROM ContextSnapshotEntity s WHERE s.projectId = :projectId ORDER BY s.versionNumber DESC LIMIT 1")
    Optional<ContextSnapshotEntity> findLatestForProject(@Param("projectId") String projectId);

    /**
     * Resolves historical context snapshots within a target date/time range.
     *
     * @param projectId the associated project ID
     * @param start start range instant
     * @param end end range instant
     * @return collection of matching ContextSnapshotEntity records
     */
    @Query("SELECT s FROM ContextSnapshotEntity s WHERE s.projectId = :projectId AND s.snapshotCreatedAt BETWEEN :start AND :end ORDER BY s.versionNumber ASC")
    Collection<ContextSnapshotEntity> findHistoryByDateRange(
        @Param("projectId") String projectId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    /**
     * Prunes and deletes historical context snapshots created before cutoff.
     *
     * @param projectId the associated project ID
     * @param cutoff cutoff threshold instant
     * @return count of deleted snapshots
     */
    @Modifying
    @Query("DELETE FROM ContextSnapshotEntity s WHERE s.projectId = :projectId AND s.snapshotCreatedAt < :cutoff")
    int pruneOldSnapshots(
        @Param("projectId") String projectId,
        @Param("cutoff") Instant cutoff
    );
}
