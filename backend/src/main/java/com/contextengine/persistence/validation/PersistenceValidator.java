package com.contextengine.persistence.validation;

import com.contextengine.persistence.entity.ContextSnapshotEntity;
import com.contextengine.persistence.entity.KnowledgeRelationshipEntity;
import com.contextengine.persistence.exception.PersistenceException;
import java.util.Objects;

/**
 * Validator class verifying database and entity invariants to ensure storage integrity.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class PersistenceValidator {

    /**
     * Validates a KnowledgeRelationshipEntity.
     * Enforces that relationships must not connect a node to itself (no self-loops).
     *
     * @param entity the relationship entity to validate
     * @throws PersistenceException if validation fails
     */
    public void validateRelationship(KnowledgeRelationshipEntity entity) {
        Objects.requireNonNull(entity, "KnowledgeRelationshipEntity must not be null");
        if (entity.getSourceNodeId() == null || entity.getTargetNodeId() == null) {
            throw new PersistenceException("Source and target node IDs must not be null");
        }
        if (entity.getSourceNodeId().equals(entity.getTargetNodeId())) {
            throw new PersistenceException("Self-referential relationships are not allowed: source must not equal target");
        }
    }

    /**
     * Validates a ContextSnapshotEntity.
     * Enforces non-negativity of metrics.
     *
     * @param entity the snapshot entity to validate
     * @throws PersistenceException if validation fails
     */
    public void validateSnapshot(ContextSnapshotEntity entity) {
        Objects.requireNonNull(entity, "ContextSnapshotEntity must not be null");
        if (entity.getTokenFootprint() < 0) {
            throw new PersistenceException("Token footprint must not be negative: " + entity.getTokenFootprint());
        }
        if (entity.getTotalFileCount() < 0) {
            throw new PersistenceException("Total file count must not be negative: " + entity.getTotalFileCount());
        }
    }
}
