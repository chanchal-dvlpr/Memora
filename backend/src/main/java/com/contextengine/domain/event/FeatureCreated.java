package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a new product feature or development milestone is registered.
 */
public record FeatureCreated(
    FeatureId featureId,
    ProjectId projectId,
    String title,
    Priority priority,
    Instant occurredAt
) implements DomainEvent {
    
    public FeatureCreated {
        Objects.requireNonNull(featureId, "FeatureId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(priority, "Priority must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Feature title must not be null or empty");
        }
    }
}
