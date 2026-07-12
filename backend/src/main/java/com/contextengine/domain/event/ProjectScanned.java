package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a scanning operation completes and a new AST model is constructed.
 */
public record ProjectScanned(
    ProjectId projectId,
    int filesScannedCount,
    int symbolsCount,
    Instant occurredAt
) implements DomainEvent {
    
    public ProjectScanned {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        if (filesScannedCount < 0) {
            throw new IllegalArgumentException("Files scanned count must be non-negative");
        }
        if (symbolsCount < 0) {
            throw new IllegalArgumentException("Symbols count must be non-negative");
        }
    }
}
