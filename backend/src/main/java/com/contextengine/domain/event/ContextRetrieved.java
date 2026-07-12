package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a retrieval query successfully returns relevant nodes.
 */
public record ContextRetrieved(
    ProjectId projectId,
    String queryText,
    int retrievedNodesCount,
    Instant occurredAt
) implements DomainEvent {
    
    public ContextRetrieved {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (queryText == null || queryText.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text must not be null or empty");
        }
        if (retrievedNodesCount < 0) {
            throw new IllegalArgumentException("Retrieved nodes count must be non-negative");
        }
    }
}
