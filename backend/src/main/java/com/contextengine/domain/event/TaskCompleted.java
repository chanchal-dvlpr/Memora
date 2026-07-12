package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.TaskId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a developer task or regression fix is successfully resolved.
 */
public record TaskCompleted(
    TaskId taskId,
    FeatureId featureId, // nullable
    ProjectId projectId,
    Instant occurredAt
) implements DomainEvent {
    
    public TaskCompleted {
        Objects.requireNonNull(taskId, "TaskId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
