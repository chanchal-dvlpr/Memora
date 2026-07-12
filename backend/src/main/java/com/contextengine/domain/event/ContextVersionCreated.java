package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.Hash;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * Logs historical context state changes and sequence markers.
 */
public record ContextVersionCreated(
    SnapshotId snapshotId,
    Version version,
    Hash deltaHash,
    Instant occurredAt
) implements DomainEvent {
    
    public ContextVersionCreated {
        Objects.requireNonNull(snapshotId, "SnapshotId must not be null");
        Objects.requireNonNull(version, "Version must not be null");
        Objects.requireNonNull(deltaHash, "Hash must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
