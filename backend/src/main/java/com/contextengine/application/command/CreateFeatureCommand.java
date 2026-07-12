package com.contextengine.application.command;

import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Command requesting to add a product feature milestone tracking node.
 * <p>
 * Related Use Case: Feature Roadmap Planning
 * Related Bounded Context / Aggregate: Project (Project Management Aggregate)
 * </p>
 */
public record CreateFeatureCommand(
    ProjectId projectId,
    String title,
    Priority priority,
    String description
) implements Command {
    
    public CreateFeatureCommand {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(priority, "Priority must not be null");
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Feature title must not be null or empty");
        }
    }
}
