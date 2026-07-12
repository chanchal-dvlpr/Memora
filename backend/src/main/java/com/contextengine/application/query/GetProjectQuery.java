package com.contextengine.application.query;

import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Query requesting to retrieve details of a specific Project.
 * <p>
 * Related Bounded Context / Aggregate: Project
 * Related Repository: ProjectRepository
 * </p>
 */
public record GetProjectQuery(
    ProjectId projectId
) implements Query {
    
    public GetProjectQuery {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
    }
}
