package com.contextengine.domain.event;

import com.contextengine.domain.entity.FeatureState;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Signals updates to feature progress metrics or states.
 */
public record FeatureUpdated(
    FeatureId featureId,
    ProjectId projectId,
    double progressPercentage,
    FeatureState status,
    Instant occurredAt
) implements DomainEvent {
    
    public FeatureUpdated {
        Objects.requireNonNull(featureId, "FeatureId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(status, "FeatureState status must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (progressPercentage < 0.0 || progressPercentage > 100.0) {
            throw new IllegalArgumentException("Progress percentage must be between 0.0 and 100.0");
        }
    }
}
