package com.contextengine.domain.entity;

import com.contextengine.domain.aggregate.AggregateRoot;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Manages the multi-dimensional semantic graph of nodes and relationship edges for a Project.
 * Exposes boundary control to protect the Zero Orphan Edges (DI-5) invariant.
 */
public class KnowledgeGraph implements AggregateRoot<ProjectId> {
    
    private final ProjectId projectId;
    private final List<KnowledgeNode> nodes;
    private final List<KnowledgeRelationship> relationships;

    /**
     * Constructs a KnowledgeGraph for the specified project.
     *
     * @param projectId the parent project ID
     */
    public KnowledgeGraph(ProjectId projectId) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.nodes = new ArrayList<>();
        this.relationships = new ArrayList<>();
    }

    @Override
    public ProjectId id() {
        return projectId;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public List<KnowledgeNode> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<KnowledgeRelationship> relationships() {
        return Collections.unmodifiableList(relationships);
    }

    /**
     * Adds a KnowledgeNode to the graph. Enforces node uniqueness.
     *
     * @param node the node to add
     */
    public void addNode(KnowledgeNode node) {
        Objects.requireNonNull(node, "Node must not be null");
        if (nodes.stream().anyMatch(n -> n.id().equals(node.id()))) {
            throw new IllegalArgumentException("Node with ID '" + node.id() + "' already exists in the graph");
        }
        nodes.add(node);
    }

    /**
     * Removes a KnowledgeNode and cascade prunes all connected relationship edges.
     * Enforces the DI-5 (Zero Orphan Edges) invariant.
     *
     * @param nodeId the ID of the node to remove
     */
    public void removeNode(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        
        // Remove the node itself
        boolean removed = nodes.removeIf(n -> n.id().equals(nodeId));
        if (!removed) {
            throw new IllegalArgumentException("Node with ID '" + nodeId + "' not found in the graph");
        }
        
        // Cascade remove all incoming and outgoing edges to protect DI-5 referential integrity
        relationships.removeIf(r -> r.sourceNodeId().equals(nodeId) || r.targetNodeId().equals(nodeId));
    }

    /**
     * Adds a directed Relationship edge between two existing nodes.
     * Enforces referential integrity: both source and target nodes must exist in the graph.
     *
     * @param relationship the edge relationship to add
     */
    public void addRelationship(KnowledgeRelationship relationship) {
        Objects.requireNonNull(relationship, "Relationship must not be null");
        
        // Enforce referential integrity: source node must exist
        boolean sourceExists = nodes.stream().anyMatch(n -> n.id().equals(relationship.sourceNodeId()));
        if (!sourceExists) {
            throw new IllegalArgumentException("Source NodeId '" + relationship.sourceNodeId() + "' does not exist in the graph");
        }
        
        // Enforce referential integrity: target node must exist
        boolean targetExists = nodes.stream().anyMatch(n -> n.id().equals(relationship.targetNodeId()));
        if (!targetExists) {
            throw new IllegalArgumentException("Target NodeId '" + relationship.targetNodeId() + "' does not exist in the graph");
        }
        
        if (relationships.stream().anyMatch(r -> r.id().equals(relationship.id()))) {
            throw new IllegalArgumentException("Relationship with ID '" + relationship.id() + "' already exists in the graph");
        }
        
        relationships.add(relationship);
    }

    /**
     * Removes a relationship by ID.
     *
     * @param relationshipId the ID of the relationship to remove
     */
    public void removeRelationship(RelationshipId relationshipId) {
        Objects.requireNonNull(relationshipId, "RelationshipId must not be null");
        boolean removed = relationships.removeIf(r -> r.id().equals(relationshipId));
        if (!removed) {
            throw new IllegalArgumentException("Relationship with ID '" + relationshipId + "' not found in the graph");
        }
    }
}
