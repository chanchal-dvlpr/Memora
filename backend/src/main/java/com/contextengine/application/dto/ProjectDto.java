package com.contextengine.application.dto;

import java.util.List;

/**
 * Data Transfer Object representing a Project.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Project
 * </p>
 *
 * @param id the unique project identifier
 * @param absoluteRootPath the filesystem directory path registered for the project
 * @param title the display title of the project
 * @param exclusions glob patterns specifying file paths excluded from indexing
 */
public record ProjectDto(
    String id,
    String absoluteRootPath,
    String title,
    List<String> exclusions
) {
    /**
     * Constructs a ProjectDto. Performs defensive copying of the exclusions list.
     */
    public ProjectDto {
        exclusions = exclusions != null ? List.copyOf(exclusions) : List.of();
    }
}
