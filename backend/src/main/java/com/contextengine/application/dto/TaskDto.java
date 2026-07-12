package com.contextengine.application.dto;

/**
 * Data Transfer Object representing a Development Task.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Task
 * </p>
 *
 * @param id the unique task identifier
 * @param projectId the associated parent project identifier
 * @param featureId the associated feature identifier, if linked to a parent feature
 * @param description description of the actionable task
 * @param status current status of the task (e.g. TODO, IN_PROGRESS, DONE)
 * @param priority priority classification level (e.g. LOW, MEDIUM, HIGH)
 */
public record TaskDto(
    String id,
    String projectId,
    String featureId,
    String description,
    String status,
    String priority
) {
}
