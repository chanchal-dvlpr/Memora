package com.contextengine.application.dto;

/**
 * Data Transfer Object representing a Project Dependency.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Dependency
 * </p>
 *
 * @param id the unique dependency identifier
 * @param projectId the associated parent project identifier
 * @param name the name of the package/library
 * @param version the semantic version string
 * @param manifestPath the relative file path to the manifest declaring this dependency
 */
public record DependencyDto(
    String id,
    String projectId,
    String name,
    String version,
    String manifestPath
) {
}
