package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.GraphWeight;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.Objects;

/**
 * Models a directed relational edge within the Knowledge Graph connecting a source Node to a target Node.
 */
public class KnowledgeRelationship {
    
    private final RelationshipId id;
    private final NodeId sourceNodeId;
    private final NodeId targetNodeId;
    private final String type;
    private GraphWeight weight;

    /**
     * Constructs a KnowledgeRelationship.
     *
     * @param id the unique relationship ID
     * @param sourceNodeId the ID of the source node
     * @param targetNodeId the ID of the target node
     * @param type the type of relationship (e.g. "CLASS_IMPLEMENTS_INTERFACE", "TASK_BLOCKS_TASK")
     * @param weight the relational strength weight parameter
     */
    public KnowledgeRelationship(RelationshipId id, NodeId sourceNodeId, NodeId targetNodeId, String type, GraphWeight weight) {
        this.id = Objects.requireNonNull(id, "RelationshipId must not be null");
        this.sourceNodeId = Objects.requireNonNull(sourceNodeId, "Source NodeId must not be null");
        this.targetNodeId = Objects.requireNonNull(targetNodeId, "Target NodeId must not be null");
        this.weight = Objects.requireNonNull(weight, "Weight must not be null");
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Relationship type must not be null or empty");
        }
        this.type = type.trim();
        
        if (sourceNodeId.equals(targetNodeId)) {
            throw new IllegalArgumentException("Self-referential relationships are not allowed (source must not equal target)");
        }
    }

    public RelationshipId id() {
        return id;
    }

    public NodeId sourceNodeId() {
        return sourceNodeId;
    }

    public NodeId targetNodeId() {
        return targetNodeId;
    }

    public String type() {
        return type;
    }

    public GraphWeight weight() {
        return weight;
    }

    /**
     * Adjusts the edge weight strength parameters.
     *
     * @param newWeight the new weight to apply
     */
    public void adjustWeight(GraphWeight newWeight) {
        this.weight = Objects.requireNonNull(newWeight, "New weight must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KnowledgeRelationship that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
