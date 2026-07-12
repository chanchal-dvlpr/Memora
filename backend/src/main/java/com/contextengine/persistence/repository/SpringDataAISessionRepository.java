package com.contextengine.persistence.repository;

import com.contextengine.persistence.entity.AISessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;

/**
 * Spring Data JPA Repository interface for AISessionEntity persistence operations.
 * <p>
 * Bounded Context: AI Ingestion
 * Related Subsystem: Persistent Storage
 * </p>
 */
public interface SpringDataAISessionRepository extends JpaRepository<AISessionEntity, String> {

    /**
     * Finds active AI conversational sessions matching a client application name.
     *
     * @param clientApp the client application identity name
     * @return collection of matching sessions
     */
    @Query("SELECT s FROM AISessionEntity s WHERE s.clientApp = :clientApp")
    Collection<AISessionEntity> findByClientIdentity(@Param("clientApp") String clientApp);
}
