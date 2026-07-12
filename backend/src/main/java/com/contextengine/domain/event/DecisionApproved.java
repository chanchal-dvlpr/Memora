package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.DecisionId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when an Architecture Decision Record (ADR) is updated to Approved.
 */
public record DecisionApproved(
    DecisionId decisionId,
    ProjectId projectId,
    Path markdownPath, // Approved by path or person
    String approvedBy,
    Instant occurredAt
) implements DomainEvent {
    
    public DecisionApproved {
        Objects.requireNonNull(decisionId, "DecisionId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(markdownPath, "Markdown path must not be null");
        Objects.requireNonNull(approvedBy, "ApprovedBy must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
