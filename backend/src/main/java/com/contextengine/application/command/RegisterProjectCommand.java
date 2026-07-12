package com.contextengine.application.command;

import com.contextengine.domain.valueobject.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Command requesting to register a new codebase directory as a Project.
 * <p>
 * Related Use Case: Project Registration
 * Related Bounded Context / Aggregate: Project
 * </p>
 */
public record RegisterProjectCommand(
    Path absoluteRootPath,
    String projectTitle,
    List<String> exclusions
) implements Command {
    
    public RegisterProjectCommand {
        Objects.requireNonNull(absoluteRootPath, "Absolute root path must not be null");
        Objects.requireNonNull(projectTitle, "Project title must not be null");
        if (projectTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Project title must not be empty");
        }
        exclusions = exclusions != null ? List.copyOf(exclusions) : Collections.emptyList();
    }
}
