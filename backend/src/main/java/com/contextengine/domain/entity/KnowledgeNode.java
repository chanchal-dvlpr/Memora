package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import java.util.Objects;

/**
 * Models a single vertex within the Knowledge Graph.
 */
public class KnowledgeNode {
    
    private final NodeId id;
    private final String type;
    private Metadata attributes;

    /**
     * Constructs a KnowledgeNode.
     *
     * @param id the unique node ID
     * @param type the type of the node (e.g. "CODE_SYMBOL", "SEMANTIC_DECISION")
     * @param attributes key-value attributes associated with the node
     */
    public KnowledgeNode(NodeId id, String type, Metadata attributes) {
        this.id = Objects.requireNonNull(id, "NodeId must not be null");
        this.attributes = Objects.requireNonNull(attributes, "Attributes must not be null");
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Node type must not be null or empty");
        }
        this.type = type.trim();
    }

    public NodeId id() {
        return id;
    }

    public String type() {
        return type;
    }

    public Metadata attributes() {
        return attributes;
    }

    /**
     * Updates the node attributes metadata.
     *
     * @param newAttributes the new attributes map
     */
    public void updateAttributes(Metadata newAttributes) {
        this.attributes = Objects.requireNonNull(newAttributes, "New attributes must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KnowledgeNode that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
