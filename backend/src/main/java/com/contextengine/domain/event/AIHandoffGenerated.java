package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a structured handoff document is compiled and delivered.
 */
public record AIHandoffGenerated(
    ProjectId projectId,
    Path handoffPath,
    int tokenCount,
    Instant occurredAt
) implements DomainEvent {
    
    public AIHandoffGenerated {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(handoffPath, "Handoff path must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (tokenCount < 0) {
            throw new IllegalArgumentException("Token count must be non-negative");
        }
    }
}
