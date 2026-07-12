package com.contextengine.application.dto;

/**
 * Data Transfer Object representing an Architecture Decision Record (ADR).
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Decision
 * </p>
 *
 * @param id the unique decision identifier
 * @param projectId the associated parent project identifier
 * @param title the title of the ADR
 * @param markdownPath the relative file path to the markdown document storing the ADR details
 * @param status current status of the decision (e.g. PROPOSED, APPROVED, REJECTED, SUPERSEDED)
 * @param supersededBy the identifier of the decision that superseded this decision, if applicable
 */
public record DecisionDto(
    String id,
    String projectId,
    String title,
    String markdownPath,
    String status,
    String supersededBy
) {
}
