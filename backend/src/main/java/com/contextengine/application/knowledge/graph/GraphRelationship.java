package com.contextengine.application.knowledge.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Directed edge entity linking nodes in the in-memory Knowledge Graph.
 */
public class GraphRelationship {

    /**
     * Supported relationship types.
     */
    public enum Type {
        CONTAINS,
        DEFINES,
        DEPENDS_ON,
        REFERENCES
    }

    private final String sourceNodeId;
    private final String targetNodeId;
    private final Type type;
    private final Map<String, Object> properties;

    /**
     * Constructs a GraphRelationship.
     *
     * @param sourceNodeId source node ID
     * @param targetNodeId target node ID
     * @param type relationship type
     * @param properties edge attributes mapping
     */
    public GraphRelationship(String sourceNodeId, String targetNodeId, Type type, Map<String, Object> properties) {
        this.sourceNodeId = Objects.requireNonNull(sourceNodeId, "SourceNodeId must not be null");
        this.targetNodeId = Objects.requireNonNull(targetNodeId, "TargetNodeId must not be null");
        this.type = Objects.requireNonNull(type, "Type must not be null");
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }

    public String sourceNodeId() {
        return sourceNodeId;
    }

    public String targetNodeId() {
        return targetNodeId;
    }

    public Type type() {
        return type;
    }

    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelationship that = (GraphRelationship) o;
        return sourceNodeId.equals(that.sourceNodeId) &&
               targetNodeId.equals(that.targetNodeId) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNodeId, targetNodeId, type);
    }
}
