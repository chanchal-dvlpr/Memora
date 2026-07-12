package com.contextengine.domain.event;

import com.contextengine.domain.service.FormatEnum;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a context payload is serialized for downstream AI consumption.
 */
public record ContextGenerated(
    SnapshotId snapshotId,
    ProjectId projectId,
    int tokenCount,
    FormatEnum outputFormat,
    Instant occurredAt
) implements DomainEvent {
    
    public ContextGenerated {
        Objects.requireNonNull(snapshotId, "SnapshotId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(outputFormat, "Output format must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (tokenCount < 0) {
            throw new IllegalArgumentException("Token count must be non-negative");
        }
    }
}
