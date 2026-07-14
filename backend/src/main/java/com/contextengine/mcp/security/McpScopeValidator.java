package com.contextengine.mcp.security;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.mcp.session.McpSession;
import java.util.Objects;

/**
 * Service executing structural boundary validation checks on project spatial workspaces.
 *
 * <p>Architecture Reference: SRS-CE-FR-020 (Acceptance Criteria AC-020-002)
 * Responsibility: Prevent clients from accessing resources or invoking actions in archived or locked projects.
 * Dependencies: {@link McpSession}, {@link Project}, {@link McpSecurityException}
 * Future Usage: Bound to resource loaders and command runners to isolate workspaces.
 */
public class McpScopeValidator {

    /**
     * Asserts that a project workspace is active and accessible.
     *
     * @param session the active client session
     * @param project the project workspace aggregate
     * @throws McpSecurityException if workspace is archived or access is blocked (-32001)
     */
    public void validateWorkspaceAccess(McpSession session, Project project) {
        Objects.requireNonNull(project, "Project must not be null");

        if (project.state() == ProjectState.ARCHIVED) {
            throw new McpSecurityException(-32001, "Workspace access blocked: Project is archived.");
        }
    }
}
