package com.contextengine.application.command;

import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Command requesting to trigger an AST scan traversal over a project workspace.
 * <p>
 * Related Use Case: Workspace Scanning
 * Related Bounded Context / Aggregate: Project / Workspace
 * </p>
 */
public record ScanProjectCommand(
    ProjectId projectId,
    boolean deep,
    boolean shouldWait
) implements Command {
    
    public ScanProjectCommand {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
    }
}
