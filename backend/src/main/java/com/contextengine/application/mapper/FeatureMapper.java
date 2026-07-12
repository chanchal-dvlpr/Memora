package com.contextengine.application.mapper;

import com.contextengine.application.dto.FeatureDto;
import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.FeatureState;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Feature domain entities to and from FeatureDto representations.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Feature
 * </p>
 */
public final class FeatureMapper {

    private FeatureMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a Feature domain entity to a FeatureDto.
     *
     * @param feature the feature domain entity
     * @return the mapped FeatureDto
     */
    public static FeatureDto toDto(Feature feature) {
        Objects.requireNonNull(feature, "Feature must not be null");
        return new FeatureDto(
            feature.id().value().toString(),
            feature.projectId().value().toString(),
            feature.title(),
            "", // No description field on the domain entity
            feature.status().name(),
            feature.priority().name()
        );
    }

    /**
     * Maps a FeatureDto to a Feature domain entity using the domain reconstruction factory.
     *
     * @param dto the feature DTO
     * @return the mapped Feature domain entity
     */
    public static Feature toDomain(FeatureDto dto) {
        Objects.requireNonNull(dto, "FeatureDto must not be null");
        return Feature.reconstruct(
            new FeatureId(UUID.fromString(dto.id())),
            new ProjectId(UUID.fromString(dto.projectId())),
            dto.title(),
            Priority.valueOf(dto.priority()),
            FeatureState.valueOf(dto.status()),
            0.0 // Defaults to 0.0, as progress is computed dynamically
        );
    }
}
