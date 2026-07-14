package com.contextengine.mcp.stub;

import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.valueobject.Direction;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class StubKnowledgeGraphRepository implements KnowledgeGraphRepository {
    @Override
    public void save(KnowledgeGraph graph) {}

    @Override
    public void commit(GraphTransaction transaction) {}

    @Override
    public Optional<KnowledgeNode> findNodeById(NodeId nodeId) {
        return Optional.empty();
    }

    @Override
    public Optional<KnowledgeRelationship> findRelationshipById(RelationshipId relationshipId) {
        return Optional.empty();
    }

    @Override
    public Collection<KnowledgeRelationship> findEdgesFrom(NodeId sourceNodeId) {
        return Collections.emptyList();
    }

    @Override
    public Collection<KnowledgeRelationship> findEdgesTo(NodeId targetNodeId) {
        return Collections.emptyList();
    }

    @Override
    public Collection<KnowledgeRelationship> findEdges(NodeId nodeId, Direction direction) {
        return Collections.emptyList();
    }

    @Override
    public Collection<KnowledgeNode> querySubGraph(NodeId centerNodeId, int depthMax) {
        return Collections.emptyList();
    }

    @Override
    public void removeNode(NodeId nodeId) {}

    @Override
    public Collection<KnowledgeNode> findNodesByProject(ProjectId projectId) {
        return Collections.emptyList();
    }

    @Override
    public Collection<KnowledgeRelationship> findRelationshipsByProject(ProjectId projectId) {
        return Collections.emptyList();
    }
}
