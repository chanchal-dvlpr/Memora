package com.contextengine.application.dto;

/**
 * Data Transfer Object representing a directed relational edge within the Knowledge Graph.
 * <p>
 * Bounded Context: Knowledge Graph Representation
 * Related Domain Aggregate: KnowledgeRelationship
 * </p>
 *
 * @param id the unique relationship identifier string
 * @param sourceNodeId the identifier string of the source node
 * @param targetNodeId the identifier string of the target node
 * @param type the relationship classification (e.g., "CALLS", "DECLARES", "DEPENDS_ON")
 * @param weight the relational connection strength value
 */
public record KnowledgeRelationshipDto(
    String id,
    String sourceNodeId,
    String targetNodeId,
    String type,
    double weight
) {
}
