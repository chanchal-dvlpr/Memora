package com.contextengine.persistence.repository;

import com.contextengine.persistence.entity.KnowledgeNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for KnowledgeNodeEntity.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Repository
public interface SpringDataKnowledgeNodeRepository extends JpaRepository<KnowledgeNodeEntity, String> {
}
