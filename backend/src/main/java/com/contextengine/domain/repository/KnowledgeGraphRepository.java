package com.contextengine.domain.repository;

import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.Direction;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.Collection;
import java.util.Optional;

/**
 * Manages transaction logging, indexing, and subgraph queries for the Knowledge Graph.
 */
public interface KnowledgeGraphRepository {
    
    /**
     * Atomic validation and synchronization of transactional updates inside the active graph model.
     *
     * @param graph the knowledge graph aggregate root
     */
    void save(KnowledgeGraph graph);
    
    /**
     * Commits a transactional batch of node and edge updates to the graph database.
     *
     * @param transaction the graph transaction batch
     */
    void commit(GraphTransaction transaction);
    
    /**
     * Fetches a specific node from the graph by its unique identifier.
     *
     * @param nodeId the node ID
     * @return an optional containing the resolved node, or empty if not found
     */
    Optional<KnowledgeNode> findNodeById(NodeId nodeId);
    
    /**
     * Resolves a specific directed edge relationship.
     *
     * @param relationshipId the relationship ID
     * @return an optional containing the resolved relationship, or empty if not found
     */
    Optional<KnowledgeRelationship> findRelationshipById(RelationshipId relationshipId);
    
    /**
     * Queries all directed edges where the source matches the provided identifier.
     *
     * @param sourceNodeId the source node ID
     * @return collection of matching directed relationship edges
     */
    Collection<KnowledgeRelationship> findEdgesFrom(NodeId sourceNodeId);
    
    /**
     * Queries all directed edges where the target matches the provided identifier.
     *
     * @param targetNodeId the target node ID
     * @return collection of matching directed relationship edges
     */
    Collection<KnowledgeRelationship> findEdgesTo(NodeId targetNodeId);
    
    /**
     * Extracts all relationship edges connected to the node in the specified direction.
     *
     * @param nodeId the node ID
     * @param direction the traversal direction filter
     * @return collection of relationship edges
     */
    Collection<KnowledgeRelationship> findEdges(NodeId nodeId, Direction direction);
    
    /**
     * Extracts a localized subgraph topological set around a focal node using a bounded hop-limit distance.
     *
     * @param centerNodeId the focal node ID
     * @param depthMax the maximum hop depth traversal distance
     * @return collection of structural nodes inside the subgraph
     */
    Collection<KnowledgeNode> querySubGraph(NodeId centerNodeId, int depthMax);
    
    /**
     * Removes a node vertex and its cascade transactional metadata from the Graph database.
     *
     * @param nodeId the node ID to delete
     */
    void removeNode(NodeId nodeId);

    /**
     * Finds all nodes belonging to a project.
     *
     * @param projectId the project ID
     * @return collection of nodes in the project
     */
    Collection<KnowledgeNode> findNodesByProject(com.contextengine.domain.valueobject.ProjectId projectId);

    /**
     * Finds all relationships belonging to a project.
     *
     * @param projectId the project ID
     * @return collection of relationships in the project
     */
    Collection<KnowledgeRelationship> findRelationshipsByProject(com.contextengine.domain.valueobject.ProjectId projectId);
}
