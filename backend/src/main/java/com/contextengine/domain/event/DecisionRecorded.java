package com.contextengine.domain.event;

import com.contextengine.domain.entity.DecisionState;
import com.contextengine.domain.valueobject.DecisionId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Logs the discovery of an ADR or markdown decision document.
 */
public record DecisionRecorded(
    DecisionId decisionId,
    ProjectId projectId,
    Path filePath,
    DecisionState status,
    Instant occurredAt
) implements DomainEvent {
    
    public DecisionRecorded {
        Objects.requireNonNull(decisionId, "DecisionId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(filePath, "File path must not be null");
        Objects.requireNonNull(status, "DecisionState status must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
