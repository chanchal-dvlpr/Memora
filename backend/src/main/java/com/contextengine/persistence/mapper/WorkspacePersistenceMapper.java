package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Workspace;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.WorkspaceId;
import com.contextengine.persistence.entity.ProjectEntity;
import com.contextengine.persistence.entity.WorkspaceEntity;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence mapper converting between Workspace domain model and WorkspaceEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class WorkspacePersistenceMapper {

    /**
     * Converts Workspace domain model to WorkspaceEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return workspace entity
     */
    public WorkspaceEntity toEntity(Workspace domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setActiveBranch(domain.activeBranch());
        entity.setActiveCommitHash(domain.activeCommitHash());
        entity.setTrackedPaths(domain.trackedPaths().stream().map(Path::value).collect(Collectors.toList()));
        return entity;
    }

    /**
     * Converts WorkspaceEntity to Workspace domain model.
     *
     * @param entity workspace entity
     * @param projectId parent project ID
     * @return workspace domain model
     */
    public Workspace toDomain(WorkspaceEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return Workspace.reconstruct(
            new WorkspaceId(UUID.fromString(entity.getId())),
            projectId,
            entity.getTrackedPaths().stream().map(Path::new).collect(Collectors.toList()),
            entity.getActiveBranch(),
            entity.getActiveCommitHash()
        );
    }
}
