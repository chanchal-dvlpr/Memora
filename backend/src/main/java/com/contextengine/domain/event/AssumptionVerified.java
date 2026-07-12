package com.contextengine.domain.event;

import com.contextengine.domain.entity.AssumptionState;
import com.contextengine.domain.valueobject.AssumptionId;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Updates the status of an active design assumption.
 */
public record AssumptionVerified(
    AssumptionId assumptionId,
    ProjectId projectId,
    AssumptionState verificationStatus,
    Instant occurredAt
) implements DomainEvent {
    
    public AssumptionVerified {
        Objects.requireNonNull(assumptionId, "AssumptionId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(verificationStatus, "Verification status must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
