package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Emitted when context compilation exceeds token limits, forcing the engine to prune secondary context data.
 * <p>
 * Bounded Context: Context Assembly
 * Architecture Reference: Event System Design (CE-EVENT-1.0) Section 6.9.8.2 (Event: context.budget_exceeded)
 * Responsibility: Transports the details of pruning actions taken to fit context inside the token window.
 * Dependencies: {@link SnapshotId}, {@link ProjectId}, {@link Metadata}.
 * </p>
 */
public record ContextBudgetExceeded(
    SnapshotId contextId,
    ProjectId projectId,
    int requestedTokens,
    int maxBudgetLimit,
    List<Metadata> pruningActionsApplied,
    Instant occurredAt
) implements DomainEvent {

    /**
     * Constructs a ContextBudgetExceeded event.
     *
     * @param contextId unique ID of the generated context
     * @param projectId associated project ID
     * @param requestedTokens total tokens requested originally
     * @param maxBudgetLimit maximum token budget limit
     * @param pruningActionsApplied pruning passes executed
     * @param occurredAt timestamp of event generation
     */
    public ContextBudgetExceeded {
        Objects.requireNonNull(contextId, "ContextId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(pruningActionsApplied, "PruningActionsApplied list must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");

        if (requestedTokens < 0) {
            throw new IllegalArgumentException("RequestedTokens must be non-negative");
        }
        if (maxBudgetLimit < 0) {
            throw new IllegalArgumentException("MaxBudgetLimit must be non-negative");
        }
    }
}
