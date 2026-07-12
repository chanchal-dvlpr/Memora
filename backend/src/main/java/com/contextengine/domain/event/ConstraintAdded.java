package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ConstraintId;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a new structural or regulatory constraint is active.
 */
public record ConstraintAdded(
    ConstraintId constraintId,
    ProjectId projectId,
    String constraintType,
    Instant occurredAt
) implements DomainEvent {
    
    public ConstraintAdded {
        Objects.requireNonNull(constraintId, "ConstraintId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (constraintType == null || constraintType.trim().isEmpty()) {
            throw new IllegalArgumentException("Constraint type must not be null or empty");
        }
    }
}
