package com.contextengine.domain.service;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Workspace;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.WorkspaceId;
import java.io.File;
import java.util.Collection;
import java.util.Objects;

/**
 * Orchestrates the secure registration, directory verification, and configuration of local project workspaces.
 */
public class ProjectRegistrationService {
    
    /**
     * Registers and initializes a Project aggregate instance.
     * Enforces the path conflict / nesting overlap rules.
     *
     * @param absoluteRootPath the root directory path
     * @param projectTitle the project title
     * @param existingProjects currently registered projects to perform overlap validation
     * @return the registered Project aggregate root
     * @throws NullPointerException if any argument is null
     * @throws DirectoryAccessDeniedException if directory does not exist or lacks write permissions
     * @throws OverlappingProjectException if root overlaps with an existing project
     */
    public Project registerProject(Path absoluteRootPath, String projectTitle, Collection<Project> existingProjects) {
        Objects.requireNonNull(absoluteRootPath, "Absolute root path must not be null");
        Objects.requireNonNull(projectTitle, "Project title must not be null");
        Objects.requireNonNull(existingProjects, "Existing projects list must not be null");
        
        File file = new File(absoluteRootPath.value());
        if (!file.exists() || !file.isDirectory()) {
            throw new DirectoryAccessDeniedException("Target workspace directory does not exist: " + absoluteRootPath.value());
        }
        if (!file.canWrite()) {
            throw new DirectoryAccessDeniedException("Target workspace lacks active write permissions: " + absoluteRootPath.value());
        }
        
        String newPath = absoluteRootPath.value();
        for (Project existing : existingProjects) {
            String existingPath = existing.rootDirectory().value();
            if (newPath.startsWith(existingPath) || existingPath.startsWith(newPath)) {
                throw new OverlappingProjectException("The target workspace path overlaps with an existing registered project: " + existing.title());
            }
        }
        
        ProjectId projectId = ProjectId.generate();
        Project project = new Project(projectId, absoluteRootPath, projectTitle);
        Workspace workspace = new Workspace(WorkspaceId.generate(), projectId);
        project.bindWorkspace(workspace);
        return project;
    }
}
