package com.contextengine.domain.repository;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Collection;
import java.util.Optional;

/**
 * Manages the persistence and retrieval of Project Aggregate Roots, workspace states, and project settings.
 */
public interface ProjectRepository {
    
    /**
     * Persists or updates the state of the Project Aggregate.
     *
     * @param project the project aggregate root to save
     */
    void save(Project project);
    
    /**
     * Resolves a Project Aggregate Root by its unique identifier.
     *
     * @param projectId the unique project ID
     * @return an optional containing the resolved Project, or empty if not found
     */
    Optional<Project> findById(ProjectId projectId);
    
    /**
     * Retrieves a Project Aggregate by matching its local filesystem root directory path.
     *
     * @param absolutePath the absolute local directory path
     * @return an optional containing the resolved Project, or empty if not found
     */
    Optional<Project> findByPath(Path absolutePath);
    
    /**
     * Returns all registered projects currently marked in the Active operational phase.
     *
     * @return collection of active registered projects
     */
    Collection<Project> findAllActive();
    
    /**
     * Permanently deletes a Project Aggregate registration from the system registry.
     *
     * @param projectId the project ID to remove
     */
    void remove(ProjectId projectId);
}
