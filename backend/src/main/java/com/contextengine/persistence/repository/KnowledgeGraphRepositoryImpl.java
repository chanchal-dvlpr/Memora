package com.contextengine.persistence.repository;

import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.valueobject.Direction;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import com.contextengine.persistence.entity.KnowledgeNodeEntity;
import com.contextengine.persistence.entity.KnowledgeRelationshipEntity;
import com.contextengine.persistence.mapper.KnowledgeNodePersistenceMapper;
import com.contextengine.persistence.mapper.KnowledgeRelationshipPersistenceMapper;
import com.contextengine.persistence.validation.PersistenceValidator;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of the KnowledgeGraphRepository application port.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class KnowledgeGraphRepositoryImpl implements KnowledgeGraphRepository {

    private final SpringDataKnowledgeNodeRepository nodeRepository;
    private final SpringDataKnowledgeRelationshipRepository relationshipRepository;
    private final KnowledgeNodePersistenceMapper nodeMapper = new KnowledgeNodePersistenceMapper();
    private final KnowledgeRelationshipPersistenceMapper relationshipMapper = new KnowledgeRelationshipPersistenceMapper();
    private final PersistenceValidator validator = new PersistenceValidator();

    /**
     * Constructs a KnowledgeGraphRepositoryImpl.
     *
     * @param nodeRepository spring data node repository
     * @param relationshipRepository spring data relationship repository
     */
    public KnowledgeGraphRepositoryImpl(
        SpringDataKnowledgeNodeRepository nodeRepository,
        SpringDataKnowledgeRelationshipRepository relationshipRepository
    ) {
        this.nodeRepository = Objects.requireNonNull(nodeRepository, "SpringDataKnowledgeNodeRepository must not be null");
        this.relationshipRepository = Objects.requireNonNull(relationshipRepository, "SpringDataKnowledgeRelationshipRepository must not be null");
    }

    @Override
    @Transactional
    public void save(KnowledgeGraph graph) {
        Objects.requireNonNull(graph, "KnowledgeGraph must not be null");
        String projectId = graph.projectId().value().toString();

        for (KnowledgeNode node : graph.nodes()) {
            KnowledgeNodeEntity entity = nodeMapper.toEntity(node, projectId);
            nodeRepository.save(entity);
        }

        for (KnowledgeRelationship relationship : graph.relationships()) {
            KnowledgeRelationshipEntity entity = relationshipMapper.toEntity(relationship, projectId);
            validator.validateRelationship(entity);
            relationshipRepository.save(entity);
        }
    }

    @Override
    @Transactional
    public void commit(GraphTransaction transaction) {
        Objects.requireNonNull(transaction, "GraphTransaction must not be null");

        String projectId = transaction.nodesToSave().stream()
            .map(n -> n.attributes().get("projectId"))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("00000000-0000-0000-0000-000000000000");

        for (KnowledgeNode node : transaction.nodesToSave()) {
            KnowledgeNodeEntity entity = nodeMapper.toEntity(node, projectId);
            nodeRepository.save(entity);
        }

        for (KnowledgeRelationship relationship : transaction.relationshipsToSave()) {
            KnowledgeRelationshipEntity entity = relationshipMapper.toEntity(relationship, projectId);
            validator.validateRelationship(entity);
            relationshipRepository.save(entity);
        }
    }

    @Override
    public Optional<KnowledgeNode> findNodeById(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        return nodeRepository.findById(nodeId.value().toString())
            .map(nodeMapper::toDomain);
    }

    @Override
    public Optional<KnowledgeRelationship> findRelationshipById(RelationshipId relationshipId) {
        Objects.requireNonNull(relationshipId, "RelationshipId must not be null");
        return relationshipRepository.findById(relationshipId.value().toString())
            .map(relationshipMapper::toDomain);
    }

    @Override
    public Collection<KnowledgeRelationship> findEdgesFrom(NodeId sourceNodeId) {
        Objects.requireNonNull(sourceNodeId, "SourceNodeId must not be null");
        return relationshipRepository.findBySourceNodeId(sourceNodeId.value().toString()).stream()
            .map(relationshipMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<KnowledgeRelationship> findEdgesTo(NodeId targetNodeId) {
        Objects.requireNonNull(targetNodeId, "TargetNodeId must not be null");
        return relationshipRepository.findByTargetNodeId(targetNodeId.value().toString()).stream()
            .map(relationshipMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<KnowledgeRelationship> findEdges(NodeId nodeId, Direction direction) {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        Objects.requireNonNull(direction, "Direction must not be null");
        String idStr = nodeId.value().toString();

        Collection<KnowledgeRelationshipEntity> entities;
        if (direction == Direction.INCOMING) {
            entities = relationshipRepository.findByTargetNodeId(idStr);
        } else if (direction == Direction.OUTGOING) {
            entities = relationshipRepository.findBySourceNodeId(idStr);
        } else {
            entities = relationshipRepository.findBySourceNodeIdOrTargetNodeId(idStr, idStr);
        }

        return entities.stream()
            .map(relationshipMapper::toDomain)
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
        findNodeById(centerNodeId).ifPresent(resultNodes::add);

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
                        findNodeById(neighbor).ifPresent(resultNodes::add);
                    }
                }
            }
        }

        return resultNodes;
    }

    @Override
    @Transactional
    public void removeNode(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        String idStr = nodeId.value().toString();
        
        // Remove connected edges to preserve referential integrity
        Collection<KnowledgeRelationshipEntity> edges = relationshipRepository.findBySourceNodeIdOrTargetNodeId(idStr, idStr);
        relationshipRepository.deleteAll(edges);
        
        nodeRepository.deleteById(idStr);
    }
}
