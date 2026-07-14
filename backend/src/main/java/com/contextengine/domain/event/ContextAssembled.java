package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Published when a context bundle is compiled and packaged for an AI assistant.
 * <p>
 * Bounded Context: Context Assembly
 * Architecture Reference: Event System Design (CE-EVENT-1.0) Section 6.9.8.1 (Event: context.assembled)
 * Responsibility: Transports the metadata snapshot of an assembled context package.
 * Dependencies: {@link SnapshotId}, {@link ProjectId}, {@link Metadata}.
 * </p>
 */
public record ContextAssembled(
    SnapshotId contextId,
    ProjectId projectId,
    List<String> focusFilesRequested,
    int tokensConsumedTotal,
    String strategyUsed,
    List<Metadata> elementsIncluded,
    Instant occurredAt
) implements DomainEvent {

    /**
     * Constructs a ContextAssembled event.
     *
     * @param contextId unique ID of the generated context
     * @param projectId associated project ID
     * @param focusFilesRequested file paths requested for developer focus
     * @param tokensConsumedTotal total token count used by elements
     * @param strategyUsed compaction strategy utilized
     * @param elementsIncluded metadata coordinates of included nodes
     * @param occurredAt timestamp of event generation
     */
    public ContextAssembled {
        Objects.requireNonNull(contextId, "ContextId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(focusFilesRequested, "FocusFilesRequested list must not be null");
        Objects.requireNonNull(strategyUsed, "StrategyUsed must not be null");
        Objects.requireNonNull(elementsIncluded, "ElementsIncluded list must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");

        if (tokensConsumedTotal < 0) {
            throw new IllegalArgumentException("TokensConsumedTotal must be non-negative");
        }
    }
}
