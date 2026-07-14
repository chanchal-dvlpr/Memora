package com.contextengine.application.knowledge.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * In-memory aggregate root representing the project knowledge graph structure.
 */
public class KnowledgeGraph {

    private final String projectId;
    private final Map<String, GraphNode> nodes;
    private final Set<GraphRelationship> relationships;
    private final KnowledgeGraphConfiguration configuration;
    private final GraphStatistics statistics;

    /**
     * Constructs a KnowledgeGraph.
     *
     * @param projectId project target identifier
     * @param configuration settings and rules config
     */
    public KnowledgeGraph(String projectId, KnowledgeGraphConfiguration configuration) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.configuration = Objects.requireNonNull(configuration, "Configuration must not be null");
        this.nodes = new HashMap<>();
        this.relationships = new HashSet<>();
        this.statistics = new GraphStatistics();
    }

    public String projectId() {
        return projectId;
    }

    public KnowledgeGraphConfiguration configuration() {
        return configuration;
    }

    public GraphStatistics statistics() {
        return statistics;
    }

    public Collection<GraphNode> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<GraphRelationship> relationships() {
        return Collections.unmodifiableCollection(relationships);
    }

    public void addNode(GraphNode node) {
        Objects.requireNonNull(node, "Node must not be null");
        nodes.put(node.id(), node);
    }

    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    public void addRelationship(GraphRelationship rel) {
        Objects.requireNonNull(rel, "Relationship must not be null");
        relationships.add(rel);
    }
}
