package com.contextengine.application.query;

import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Query requesting the current status and metrics of a project workspace scan.
 * <p>
 * Related Bounded Context / Aggregate: Project / Workspace
 * Related Service: ProjectScannerService
 * </p>
 */
public record GetScanStatusQuery(
    ProjectId projectId
) implements Query {
    
    public GetScanStatusQuery {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
    }
}
