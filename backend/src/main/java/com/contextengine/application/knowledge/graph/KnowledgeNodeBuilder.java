package com.contextengine.application.knowledge.graph;

import java.util.Map;
import java.util.Objects;

/**
 * Node creation helper that prevents duplicates and updates graph counts safely.
 */
public class KnowledgeNodeBuilder {

    private final KnowledgeGraph graph;

    /**
     * Constructs a KnowledgeNodeBuilder.
     *
     * @param graph target KnowledgeGraph to build nodes in
     */
    public KnowledgeNodeBuilder(KnowledgeGraph graph) {
        this.graph = Objects.requireNonNull(graph, "Graph must not be null");
    }

    /**
     * Creates a new GraphNode in the graph or returns the existing node if ID already exists.
     *
     * @param id unique node identifier
     * @param type node classification type
     * @param label human-readable name or label
     * @param properties map of structural attributes
     * @return the created or existing GraphNode
     */
    public GraphNode createOrGetNode(String id, GraphNode.Type type, String label, Map<String, Object> properties) {
        Objects.requireNonNull(id, "ID must not be null");
        Objects.requireNonNull(type, "Type must not be null");
        Objects.requireNonNull(label, "Label must not be null");

        GraphNode existing = graph.getNode(id);
        if (existing != null) {
            return existing;
        }

        GraphNode node = new GraphNode(id, type, label, properties);
        graph.addNode(node);
        graph.statistics().setNodeCount(graph.nodes().size());
        return node;
    }
}
