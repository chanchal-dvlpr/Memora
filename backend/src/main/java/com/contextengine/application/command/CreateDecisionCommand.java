package com.contextengine.application.command;

import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Command requesting to log a new Architecture Decision Record (ADR).
 * <p>
 * Related Use Case: ADR Architecture Logging
 * Related Bounded Context / Aggregate: Project (Project Management Aggregate)
 * </p>
 */
public record CreateDecisionCommand(
    ProjectId projectId,
    String title,
    Path markdownPath,
    String rationale,
    String consequences
) implements Command {
    
    public CreateDecisionCommand {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(markdownPath, "Markdown path must not be null");
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Decision title must not be null or empty");
        }
    }
}
