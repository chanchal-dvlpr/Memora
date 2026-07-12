package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * Logs the serialization and persistence of a context snapshot.
 */
public record ContextSnapshotCreated(
    SnapshotId snapshotId,
    ProjectId projectId,
    Version graphVersion,
    Instant occurredAt
) implements DomainEvent {
    
    public ContextSnapshotCreated {
        Objects.requireNonNull(snapshotId, "SnapshotId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(graphVersion, "Version must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
