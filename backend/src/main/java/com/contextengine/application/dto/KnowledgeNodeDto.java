package com.contextengine.application.dto;

import java.util.Map;

/**
 * Data Transfer Object representing a vertex within the Knowledge Graph.
 * <p>
 * Bounded Context: Knowledge Graph Representation
 * Related Domain Aggregate: KnowledgeNode
 * </p>
 *
 * @param id the unique node identifier string
 * @param type the semantic classification type (e.g., "FILE", "CLASS", "METHOD")
 * @param attributes unstructured key-value properties associated with the node
 */
public record KnowledgeNodeDto(
    String id,
    String type,
    Map<String, String> attributes
) {
    /**
     * Constructs a KnowledgeNodeDto. Performs defensive copying of the attributes map.
     */
    public KnowledgeNodeDto {
        attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
    }
}
