package com.contextengine.application.mapper;

import com.contextengine.application.dto.WorkspaceDto;
import com.contextengine.domain.entity.Workspace;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.WorkspaceId;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Workspace domain entities to and from WorkspaceDto representations.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Domain Aggregate: Workspace
 * </p>
 */
public final class WorkspaceMapper {

    private WorkspaceMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a Workspace domain entity to a WorkspaceDto.
     *
     * @param workspace the workspace domain entity
     * @return the mapped WorkspaceDto
     */
    public static WorkspaceDto toDto(Workspace workspace) {
        Objects.requireNonNull(workspace, "Workspace must not be null");
        return new WorkspaceDto(
            workspace.id().value().toString(),
            workspace.projectId().value().toString(),
            workspace.trackedPaths().stream().map(Path::value).toList(),
            workspace.activeBranch(),
            workspace.activeCommitHash()
        );
    }

    /**
     * Maps a WorkspaceDto to a Workspace domain entity.
     *
     * @param dto the workspace DTO
     * @return the mapped Workspace domain entity
     */
    public static Workspace toDomain(WorkspaceDto dto) {
        Objects.requireNonNull(dto, "WorkspaceDto must not be null");
        Workspace workspace = new Workspace(
            new WorkspaceId(UUID.fromString(dto.id())),
            new ProjectId(UUID.fromString(dto.projectId()))
        );
        if (dto.trackedPaths() != null) {
            for (String path : dto.trackedPaths()) {
                workspace.trackPath(new Path(path));
            }
        }
        workspace.updateGitMetadata(dto.activeBranch(), dto.activeCommitHash());
        return workspace;
    }
}
