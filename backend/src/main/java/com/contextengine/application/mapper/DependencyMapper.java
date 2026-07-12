package com.contextengine.application.mapper;

import com.contextengine.application.dto.DependencyDto;
import com.contextengine.domain.entity.Dependency;
import com.contextengine.domain.valueobject.DependencyId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SemanticVersion;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Dependency domain entities to and from DependencyDto representations.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Dependency
 * </p>
 */
public final class DependencyMapper {

    private DependencyMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a Dependency domain entity to a DependencyDto.
     *
     * @param dependency the dependency domain entity
     * @return the mapped DependencyDto
     */
    public static DependencyDto toDto(Dependency dependency) {
        Objects.requireNonNull(dependency, "Dependency must not be null");
        return new DependencyDto(
            dependency.id().value().toString(),
            dependency.projectId().value().toString(),
            dependency.packageName(),
            dependency.version().value(),
            dependency.manifestPath().value()
        );
    }

    /**
     * Maps a DependencyDto to a Dependency domain entity.
     *
     * @param dto the dependency DTO
     * @return the mapped Dependency domain entity
     */
    public static Dependency toDomain(DependencyDto dto) {
        Objects.requireNonNull(dto, "DependencyDto must not be null");
        return new Dependency(
            new DependencyId(UUID.fromString(dto.id())),
            new ProjectId(UUID.fromString(dto.projectId())),
            dto.name(),
            new SemanticVersion(dto.version()),
            new Path(dto.manifestPath())
        );
    }
}
