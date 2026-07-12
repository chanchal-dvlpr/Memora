package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.DependencyId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SemanticVersion;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when package manifest modifications are detected.
 */
public record DependencyUpdated(
    DependencyId dependencyId,
    ProjectId projectId,
    String packageName,
    SemanticVersion oldVersion,
    SemanticVersion newVersion,
    Instant occurredAt
) implements DomainEvent {
    
    public DependencyUpdated {
        Objects.requireNonNull(dependencyId, "DependencyId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(oldVersion, "Old version must not be null");
        Objects.requireNonNull(newVersion, "New version must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name must not be null or empty");
        }
    }
}
