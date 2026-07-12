package com.contextengine.application.dto;

/**
 * Data Transfer Object representing an assembled Context Snapshot.
 * <p>
 * Bounded Context: Context Assembly
 * Related Domain Aggregate: ContextSnapshot
 * </p>
 *
 * @param id the unique snapshot identifier
 * @param projectId the associated parent project identifier
 * @param payload the compiled, markdown or raw context text payload
 * @param tokensUsed the total token footprint count used by this context payload
 * @param timestamp the creation timestamp string
 */
public record ContextSnapshotDto(
    String id,
    String projectId,
    String payload,
    int tokensUsed,
    String timestamp
) {
}
