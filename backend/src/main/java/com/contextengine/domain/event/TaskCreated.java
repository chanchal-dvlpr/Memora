package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.TaskId;
import java.time.Instant;
import java.util.Objects;

/**
 * Signals the creation of an actionable coding task or TODO.
 */
public record TaskCreated(
    TaskId taskId,
    FeatureId featureId, // nullable
    ProjectId projectId,
    Priority taskPriority,
    Instant occurredAt
) implements DomainEvent {
    
    public TaskCreated {
        Objects.requireNonNull(taskId, "TaskId must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(taskPriority, "Priority must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
