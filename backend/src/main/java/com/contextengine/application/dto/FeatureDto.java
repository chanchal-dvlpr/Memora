package com.contextengine.application.dto;

/**
 * Data Transfer Object representing a Milestone Feature.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Feature
 * </p>
 *
 * @param id the unique feature identifier
 * @param projectId the associated parent project identifier
 * @param title the title of the feature
 * @param description detailed description of the feature goal
 * @param status current status of the feature (e.g. PROPOSED, ACTIVE, COMPLETED)
 * @param priority priority classification level (e.g. LOW, MEDIUM, HIGH)
 */
public record FeatureDto(
    String id,
    String projectId,
    String title,
    String description,
    String status,
    String priority
) {
}
