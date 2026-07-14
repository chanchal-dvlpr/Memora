package com.contextengine.application.knowledge.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Node entity in the in-memory Knowledge Graph.
 */
public class GraphNode {

    /**
     * Supported node categories in the graph.
     */
    public enum Type {
        PROJECT,
        WORKSPACE,
        MODULE,
        DIRECTORY,
        FILE,
        SYMBOL,
        DEPENDENCY
    }

    private final String id;
    private final Type type;
    private final String label;
    private final Map<String, Object> properties;

    /**
     * Constructs a GraphNode.
     *
     * @param id unique node identifier
     * @param type node classification type
     * @param label human-readable name or label
     * @param properties map of structural attributes
     */
    public GraphNode(String id, Type type, String label, Map<String, Object> properties) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.type = Objects.requireNonNull(type, "Type must not be null");
        this.label = Objects.requireNonNull(label, "Label must not be null");
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }

    public String id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public String label() {
        return label;
    }

    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return id.equals(graphNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
