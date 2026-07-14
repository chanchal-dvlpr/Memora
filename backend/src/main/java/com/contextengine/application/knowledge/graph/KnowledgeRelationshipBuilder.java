package com.contextengine.application.knowledge.graph;

import java.util.Map;
import java.util.Objects;

/**
 * Directed edge creation helper enforcing boundary presence and preventing duplicate entries.
 */
public class KnowledgeRelationshipBuilder {

    private final KnowledgeGraph graph;

    /**
     * Constructs a KnowledgeRelationshipBuilder.
     *
     * @param graph target KnowledgeGraph
     */
    public KnowledgeRelationshipBuilder(KnowledgeGraph graph) {
        this.graph = Objects.requireNonNull(graph, "Graph must not be null");
    }

    /**
     * Creates a new relationship between existing source and target nodes, enforcing validations.
     *
     * @param sourceNodeId source node ID
     * @param targetNodeId target node ID
     * @param type relationship type
     * @param properties edge properties
     * @return the created GraphRelationship
     */
    public GraphRelationship createOrGetRelationship(
        String sourceNodeId,
        String targetNodeId,
        GraphRelationship.Type type,
        Map<String, Object> properties
    ) {
        Objects.requireNonNull(sourceNodeId, "SourceNodeId must not be null");
        Objects.requireNonNull(targetNodeId, "TargetNodeId must not be null");
        Objects.requireNonNull(type, "Type must not be null");

        // Create relationship and register in graph (Set automatically prevents duplicates)
        GraphRelationship rel = new GraphRelationship(sourceNodeId, targetNodeId, type, properties);
        graph.addRelationship(rel);
        graph.statistics().setRelationshipCount(graph.relationships().size());

        return rel;
    }
}
