package com.contextengine.application.mapper;

import com.contextengine.application.dto.ProjectDto;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Project entities to and from ProjectDto representations.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Project
 * </p>
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a Project domain entity to a ProjectDto.
     *
     * @param project the project domain entity
     * @return the mapped ProjectDto
     */
    public static ProjectDto toDto(Project project) {
        Objects.requireNonNull(project, "Project must not be null");
        return new ProjectDto(
            project.id().value().toString(),
            project.rootDirectory().value(),
            project.title(),
            List.of()
        );
    }

    /**
     * Maps a ProjectDto to a Project domain entity.
     *
     * @param dto the project DTO
     * @return the mapped Project domain entity
     */
    public static Project toDomain(ProjectDto dto) {
        Objects.requireNonNull(dto, "ProjectDto must not be null");
        return new Project(
            new ProjectId(UUID.fromString(dto.id())),
            new Path(dto.absoluteRootPath()),
            dto.title()
        );
    }
}
