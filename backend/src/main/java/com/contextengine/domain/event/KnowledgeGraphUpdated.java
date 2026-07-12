package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when graph transactional writes commit successfully.
 */
public record KnowledgeGraphUpdated(
    ProjectId projectId,
    int nodesAddedCount,
    int edgesAddedCount,
    Instant occurredAt
) implements DomainEvent {
    
    public KnowledgeGraphUpdated {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (nodesAddedCount < 0) {
            throw new IllegalArgumentException("Nodes added count must be non-negative");
        }
        if (edgesAddedCount < 0) {
            throw new IllegalArgumentException("Edges added count must be non-negative");
        }
    }
}
