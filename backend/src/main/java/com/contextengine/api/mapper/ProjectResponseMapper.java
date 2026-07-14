package com.contextengine.api.mapper;

import com.contextengine.api.response.ProjectResponse;
import com.contextengine.application.dto.ProjectDto;
import java.time.Instant;
import java.util.Objects;

/**
 * Maps Project Application DTOs to REST response models.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.1 (Project Registry Engine)
 * </p>
 */
public final class ProjectResponseMapper {

    private ProjectResponseMapper() {
        // Prevent instantiation
    }

    /**
     * Transforms a ProjectDto to ProjectResponse.
     *
     * @param dto the application project DTO payload
     * @return REST project response model
     */
    public static ProjectResponse toResponse(ProjectDto dto) {
        Objects.requireNonNull(dto, "ProjectDto must not be null");
        return new ProjectResponse(
            dto.id(),
            dto.title(),
            dto.absoluteRootPath(),
            dto.exclusions(),
            "ACTIVE",
            Instant.now().toString(),
            1
        );
    }
}
