package com.contextengine.application.query;

import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Query requesting to retrieve the most recently generated context snapshot for a Project.
 * <p>
 * Related Bounded Context / Aggregate: Context
 * Related Repository: ContextRepository
 * </p>
 */
public record GetLatestSnapshotQuery(
    ProjectId projectId
) implements Query {
    
    public GetLatestSnapshotQuery {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
    }
}
