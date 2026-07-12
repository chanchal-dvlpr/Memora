package com.contextengine.application.command;

import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Command requesting to add a development task or TODO.
 * <p>
 * Related Use Case: Task Lifecycle Tracking
 * Related Bounded Context / Aggregate: Project (Project Management Aggregate)
 * </p>
 */
public record CreateTaskCommand(
    ProjectId projectId,
    FeatureId featureId, // nullable
    String description,
    Priority priority
) implements Command {
    
    public CreateTaskCommand {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(priority, "Priority must not be null");
        
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description must not be null or empty");
        }
    }
}
