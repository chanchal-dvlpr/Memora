package com.contextengine.persistence.repository;

import com.contextengine.persistence.entity.KnowledgeRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;

/**
 * Spring Data JPA repository for KnowledgeRelationshipEntity.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Repository
public interface SpringDataKnowledgeRelationshipRepository extends JpaRepository<KnowledgeRelationshipEntity, String> {

    /**
     * Finds relationships originating from a source node.
     *
     * @param sourceNodeId source node ID
     * @return collection of relationships
     */
    Collection<KnowledgeRelationshipEntity> findBySourceNodeId(String sourceNodeId);

    /**
     * Finds relationships pointing to a target node.
     *
     * @param targetNodeId target node ID
     * @return collection of relationships
     */
    Collection<KnowledgeRelationshipEntity> findByTargetNodeId(String targetNodeId);

    /**
     * Finds relationships connected to a node as source or target.
     *
     * @param sourceNodeId source node ID
     * @param targetNodeId target node ID
     * @return collection of relationships
     */
    Collection<KnowledgeRelationshipEntity> findBySourceNodeIdOrTargetNodeId(String sourceNodeId, String targetNodeId);

    /**
     * Finds relationships belonging to a project.
     *
     * @param projectId project ID
     * @return collection of relationships
     */
    Collection<KnowledgeRelationshipEntity> findByProjectId(String projectId);
}
