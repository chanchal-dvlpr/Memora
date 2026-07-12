package com.contextengine.infrastructure.graph;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Technical graph storage component managing vertices and directed edges in-memory.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Subsystem (KG-SUB)
 * </p>
 */
public class InMemoryGraphStorage {

    private final Map<NodeId, KnowledgeNode> nodes = new ConcurrentHashMap<>();
    private final Map<RelationshipId, KnowledgeRelationship> edges = new ConcurrentHashMap<>();

    /**
     * Adds a node to the storage.
     *
     * @param node the knowledge node to add
     */
    public void addNode(KnowledgeNode node) {
        Objects.requireNonNull(node, "Node must not be null");
        nodes.put(node.id(), node);
    }

    /**
     * Removes a node and its connected edges from the storage.
     *
     * @param id the unique node ID
     */
    public void removeNode(NodeId id) {
        Objects.requireNonNull(id, "NodeId must not be null");
        nodes.remove(id);
        edges.values().removeIf(edge -> edge.sourceNodeId().equals(id) || edge.targetNodeId().equals(id));
    }

    /**
     * Resolves a node by its ID.
     *
     * @param id the unique node ID
     * @return optional containing the node, or empty
     */
    public Optional<KnowledgeNode> getNode(NodeId id) {
        Objects.requireNonNull(id, "NodeId must not be null");
        return Optional.ofNullable(nodes.get(id));
    }

    /**
     * Adds an edge relationship to the storage.
     *
     * @param edge the directed relationship edge to add
     */
    public void addEdge(KnowledgeRelationship edge) {
        Objects.requireNonNull(edge, "Relationship must not be null");
        edges.put(edge.id(), edge);
    }

    /**
     * Resolves an edge relationship by its ID.
     *
     * @param id the unique relationship ID
     * @return optional containing the relationship, or empty
     */
    public Optional<KnowledgeRelationship> getEdge(RelationshipId id) {
        Objects.requireNonNull(id, "RelationshipId must not be null");
        return Optional.ofNullable(edges.get(id));
    }

    /**
     * Returns all nodes in the storage.
     *
     * @return unmodifiable collection of nodes
     */
    public Collection<KnowledgeNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Returns all relationship edges in the storage.
     *
     * @return unmodifiable collection of relationships
     */
    public Collection<KnowledgeRelationship> getAllEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    /**
     * Clears all storage content.
     */
    public void clear() {
        nodes.clear();
        edges.clear();
    }
}
