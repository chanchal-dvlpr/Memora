package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.BugId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a functional discrepancy or test suite failure is registered.
 */
public record BugDetected(
    BugId bugId,
    ProjectId projectId,
    Path sourcePath,
    String commitHash,
    Instant occurredAt
) implements DomainEvent {
    
    public BugDetected {
        Objects.requireNonNull(bugId, "BugId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        // At least one descriptor must exist
        if (sourcePath == null && (commitHash == null || commitHash.trim().isEmpty())) {
            throw new IllegalArgumentException("BugDetected must contain at least one source file path or commit hash");
        }
    }
}
