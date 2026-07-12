package com.contextengine.persistence;

import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.valueobject.Direction;
import com.contextengine.domain.valueobject.GraphWeight;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.RelationshipId;
import com.contextengine.persistence.exception.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GraphPersistenceTest {

    @Autowired
    private KnowledgeGraphRepository graphRepository;

    @Test
    void testSaveAndRetrieveGraph() {
        ProjectId projectId = ProjectId.generate();
        KnowledgeGraph graph = new KnowledgeGraph(projectId);

        KnowledgeNode node1 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Node1")));
        KnowledgeNode node2 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Node2")));
        KnowledgeRelationship edge = new KnowledgeRelationship(RelationshipId.generate(), node1.id(), node2.id(), "DEPENDS_ON", new GraphWeight(1.5));

        graph.addNode(node1);
        graph.addNode(node2);
        graph.addRelationship(edge);

        graphRepository.save(graph);

        assertThat(graphRepository.findNodeById(node1.id())).isPresent();
        assertThat(graphRepository.findNodeById(node2.id())).isPresent();
        assertThat(graphRepository.findRelationshipById(edge.id())).isPresent();

        Collection<KnowledgeRelationship> outgoing = graphRepository.findEdgesFrom(node1.id());
        assertThat(outgoing).hasSize(1);
        assertThat(outgoing.iterator().next().targetNodeId()).isEqualTo(node2.id());

        Collection<KnowledgeRelationship> incoming = graphRepository.findEdgesTo(node2.id());
        assertThat(incoming).hasSize(1);
        assertThat(incoming.iterator().next().sourceNodeId()).isEqualTo(node1.id());
    }

    @Test
    void testCommitTransaction() {
        KnowledgeNode node = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "TxNode", "projectId", ProjectId.generate().value().toString())));
        GraphTransaction tx = new GraphTransaction(List.of(node), List.of());

        graphRepository.commit(tx);

        assertThat(graphRepository.findNodeById(node.id())).isPresent();
    }

    @Test
    void testSubGraphTraversal() {
        ProjectId projectId = ProjectId.generate();
        KnowledgeGraph graph = new KnowledgeGraph(projectId);

        KnowledgeNode center = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Center")));
        KnowledgeNode nodeA = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "NodeA")));
        KnowledgeNode nodeB = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "NodeB")));

        KnowledgeRelationship edge1 = new KnowledgeRelationship(RelationshipId.generate(), center.id(), nodeA.id(), "DEPENDS_ON", new GraphWeight(1.0));
        KnowledgeRelationship edge2 = new KnowledgeRelationship(RelationshipId.generate(), nodeA.id(), nodeB.id(), "DEPENDS_ON", new GraphWeight(1.0));

        graph.addNode(center);
        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addRelationship(edge1);
        graph.addRelationship(edge2);

        graphRepository.save(graph);

        Collection<KnowledgeNode> sub1 = graphRepository.querySubGraph(center.id(), 1);
        assertThat(sub1).containsExactlyInAnyOrder(center, nodeA);

        Collection<KnowledgeNode> sub2 = graphRepository.querySubGraph(center.id(), 2);
        assertThat(sub2).containsExactlyInAnyOrder(center, nodeA, nodeB);
    }

    @Test
    void testRelationshipValidationFailure() {
        NodeId nodeId = NodeId.generate();
        String projIdStr = ProjectId.generate().value().toString();

        KnowledgeNode node = new KnowledgeNode(nodeId, "CODE_SYMBOL", new Metadata(Map.of("name", "SelfLoopNode", "projectId", projIdStr)));
        
        // Construct invalid self-referential relationship entity directly to trigger persistence validation
        com.contextengine.persistence.entity.KnowledgeRelationshipEntity invalidEntity = new com.contextengine.persistence.entity.KnowledgeRelationshipEntity();
        invalidEntity.setId(RelationshipId.generate().value().toString());
        invalidEntity.setProjectId(projIdStr);
        invalidEntity.setSourceNodeId(nodeId.value().toString());
        invalidEntity.setTargetNodeId(nodeId.value().toString()); // Self loop!
        invalidEntity.setRelationshipType("DEPENDS_ON");
        invalidEntity.setCallFrequency(1);

        com.contextengine.persistence.validation.PersistenceValidator validator = new com.contextengine.persistence.validation.PersistenceValidator();
        assertThatThrownBy(() -> validator.validateRelationship(invalidEntity))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Self-referential relationships are not allowed");
    }
}
