package com.contextengine.persistence.repository;

import com.contextengine.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for EventEntity persistence operations.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
public interface SpringDataEventRepository extends JpaRepository<EventEntity, String> {

    /**
     * Finds events chronologically by project ID starting from a sequence number.
     *
     * @param projectId project ID string, or null for system-wide events
     * @param startSeq starting sequence number (inclusive)
     * @return list of EventEntity objects ordered by sequence number
     */
    @Query("SELECT e FROM EventEntity e WHERE (:projectId IS NULL OR e.projectId = :projectId) AND e.sequenceNum >= :startSeq ORDER BY e.sequenceNum ASC")
    List<EventEntity> findEvents(@Param("projectId") String projectId, @Param("startSeq") Long startSeq);

    /**
     * Resolves the maximum sequence number for a project.
     *
     * @param projectId project ID string, or null for system-wide events
     * @return optional maximum sequence number
     */
    @Query("SELECT MAX(e.sequenceNum) FROM EventEntity e WHERE (:projectId IS NULL OR e.projectId = :projectId)")
    Optional<Long> findMaxSequenceNum(@Param("projectId") String projectId);
}
