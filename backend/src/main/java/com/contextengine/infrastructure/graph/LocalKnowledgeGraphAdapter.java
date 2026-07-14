package com.contextengine.infrastructure.graph;

import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.valueobject.Direction;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Knowledge Graph infrastructure adapter implementing the domain repository contract.
 * Manages technical persistence, indexing, and subgraph query traversals.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Port: KnowledgeGraphRepository
 * Related Subsystem: Knowledge Graph Subsystem (KG-SUB)
 * </p>
 */
public class LocalKnowledgeGraphAdapter implements KnowledgeGraphRepository {

    private final InMemoryGraphStorage storage;

    /**
     * Constructs a LocalKnowledgeGraphAdapter.
     *
     * @param storage technical graph storage component
     */
    public LocalKnowledgeGraphAdapter(InMemoryGraphStorage storage) {
        this.storage = Objects.requireNonNull(storage, "InMemoryGraphStorage must not be null");
    }

    @Override
    public void save(KnowledgeGraph graph) {
        Objects.requireNonNull(graph, "KnowledgeGraph must not be null");
        graph.nodes().forEach(storage::addNode);
        graph.relationships().forEach(storage::addEdge);
    }

    @Override
    public void commit(GraphTransaction transaction) {
        Objects.requireNonNull(transaction, "GraphTransaction must not be null");
        transaction.nodesToSave().forEach(storage::addNode);
        transaction.relationshipsToSave().forEach(storage::addEdge);
    }

    @Override
    public Optional<KnowledgeNode> findNodeById(NodeId nodeId) {
        return storage.getNode(nodeId);
    }

    @Override
    public Optional<KnowledgeRelationship> findRelationshipById(RelationshipId relationshipId) {
        return storage.getEdge(relationshipId);
    }

    @Override
    public Collection<KnowledgeRelationship> findEdgesFrom(NodeId sourceNodeId) {
        Objects.requireNonNull(sourceNodeId, "Source node ID must not be null");
        return storage.getAllEdges().stream()
            .filter(edge -> edge.sourceNodeId().equals(sourceNodeId))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<KnowledgeRelationship> findEdgesTo(NodeId targetNodeId) {
        Objects.requireNonNull(targetNodeId, "Target node ID must not be null");
        return storage.getAllEdges().stream()
            .filter(edge -> edge.targetNodeId().equals(targetNodeId))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<KnowledgeRelationship> findEdges(NodeId nodeId, Direction direction) {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        Objects.requireNonNull(direction, "Direction must not be null");
        return storage.getAllEdges().stream()
            .filter(edge -> {
                boolean isSource = edge.sourceNodeId().equals(nodeId);
                boolean isTarget = edge.targetNodeId().equals(nodeId);
                return (direction == Direction.OUTGOING && isSource) ||
                       (direction == Direction.INCOMING && isTarget) ||
                       (direction == Direction.BOTH && (isSource || isTarget));
            })
            .collect(Collectors.toList());
    }

    @Override
    public Collection<KnowledgeNode> querySubGraph(NodeId centerNodeId, int depthMax) {
        Objects.requireNonNull(centerNodeId, "CenterNodeId must not be null");
        if (depthMax < 0) {
            throw new IllegalArgumentException("Depth must be non-negative");
        }

        Set<NodeId> visited = new HashSet<>();
        Queue<NodeId> queue = new LinkedList<>();
        Map<NodeId, Integer> depths = new HashMap<>();

        queue.add(centerNodeId);
        visited.add(centerNodeId);
        depths.put(centerNodeId, 0);

        List<KnowledgeNode> resultNodes = new ArrayList<>();
        storage.getNode(centerNodeId).ifPresent(resultNodes::add);

        while (!queue.isEmpty()) {
            NodeId current = queue.poll();
            int currentDepth = depths.get(current);

            if (currentDepth < depthMax) {
                Collection<KnowledgeRelationship> connectedEdges = findEdges(current, Direction.BOTH);
                for (KnowledgeRelationship edge : connectedEdges) {
                    NodeId neighbor = edge.sourceNodeId().equals(current) ? edge.targetNodeId() : edge.sourceNodeId();
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        depths.put(neighbor, currentDepth + 1);
                        queue.add(neighbor);
                        storage.getNode(neighbor).ifPresent(resultNodes::add);
                    }
                }
            }
        }

        return resultNodes;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        storage.removeNode(nodeId);
    }

    @Override
    public Collection<KnowledgeNode> findNodesByProject(com.contextengine.domain.valueobject.ProjectId projectId) {
        String projectIdStr = projectId.value().toString();
        return storage.getAllNodes().stream()
            .filter(node -> projectIdStr.equals(node.attributes().get("projectId")))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<KnowledgeRelationship> findRelationshipsByProject(com.contextengine.domain.valueobject.ProjectId projectId) {
        String projectIdStr = projectId.value().toString();
        Set<NodeId> nodeIds = findNodesByProject(projectId).stream()
            .map(KnowledgeNode::id)
            .collect(Collectors.toSet());
        return storage.getAllEdges().stream()
            .filter(edge -> nodeIds.contains(edge.sourceNodeId()) && nodeIds.contains(edge.targetNodeId()))
            .collect(Collectors.toList());
    }
}
