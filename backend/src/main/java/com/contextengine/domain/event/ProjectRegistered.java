package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a local codebase directory is successfully registered as a Project and initialized.
 */
public record ProjectRegistered(
    ProjectId projectId,
    Path absoluteRootPath,
    Instant occurredAt
) implements DomainEvent {
    
    public ProjectRegistered {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(absoluteRootPath, "Absolute root path must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
