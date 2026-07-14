package com.contextengine.security.isolation;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Service enforcing logical project boundaries and workspace isolation rules.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Workspace Isolation Service)
 * Responsibility: Verify project active state and delegate filesystem containment checks to WorkspaceBoundaryValidator.
 * Dependencies: {@link ProjectRepository}, {@link WorkspaceBoundaryValidator}, {@link SecurityConstants}, {@link SecurityException}
 * Future Usage: Queried by ingestion pipelines, scanner modules, or context engines prior to accessing files.
 * </p>
 */
public class WorkspaceIsolationService {

    private final ProjectRepository projectRepository;
    private final WorkspaceBoundaryValidator boundaryValidator;

    /**
     * Constructs a WorkspaceIsolationService.
     *
     * @param projectRepository repository to load project workspace states
     * @param boundaryValidator validator performing path containment check
     */
    public WorkspaceIsolationService(
        ProjectRepository projectRepository,
        WorkspaceBoundaryValidator boundaryValidator
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
        this.boundaryValidator = Objects.requireNonNull(boundaryValidator, "WorkspaceBoundaryValidator must not be null");
    }

    /**
     * Asserts that a target path can be accessed securely within a project's registered boundary.
     *
     * @param projectId the target project ID
     * @param targetPath the requested target path value object
     * @throws SecurityException if the project is missing, archived, or path escapes boundaries
     */
    public void validateAccess(ProjectId projectId, com.contextengine.domain.valueobject.Path targetPath) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(targetPath, "Target path must not be null");

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new SecurityException(
                SecurityConstants.ERROR_WORKSPACE_LOCKED,
                "Access blocked: Project registration not found for ID: " + projectId.value()
            ));

        if (project.state() == ProjectState.ARCHIVED) {
            throw new SecurityException(
                SecurityConstants.ERROR_WORKSPACE_LOCKED,
                "Access blocked: Project workspace is archived: " + projectId.value()
            );
        }

        java.nio.file.Path rootPath = Paths.get(project.rootDirectory().value());
        java.nio.file.Path targetFilePath = Paths.get(targetPath.value());

        boundaryValidator.validateBoundary(rootPath, targetFilePath);
    }
}
