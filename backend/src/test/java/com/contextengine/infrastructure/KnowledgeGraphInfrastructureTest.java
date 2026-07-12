package com.contextengine.infrastructure;

import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.GraphWeight;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import com.contextengine.infrastructure.graph.InMemoryGraphStorage;
import com.contextengine.infrastructure.graph.LocalKnowledgeGraphAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeGraphInfrastructureTest {

    private InMemoryGraphStorage storage;
    private LocalKnowledgeGraphAdapter adapter;

    @BeforeEach
    void setUp() {
        storage = new InMemoryGraphStorage();
        adapter = new LocalKnowledgeGraphAdapter(storage);
    }

    @Test
    void testCommitAndRetrieve() {
        KnowledgeNode node1 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Node1")));
        KnowledgeNode node2 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Node2")));
        KnowledgeRelationship edge = new KnowledgeRelationship(RelationshipId.generate(), node1.id(), node2.id(), "DEPENDS_ON", new GraphWeight(1.0));

        GraphTransaction tx = new GraphTransaction(List.of(node1, node2), List.of(edge));
        adapter.commit(tx);

        assertThat(adapter.findNodeById(node1.id())).isPresent();
        assertThat(adapter.findNodeById(node2.id())).isPresent();
        assertThat(adapter.findRelationshipById(edge.id())).isPresent();
    }

    @Test
    void testSubGraphTraversal() {
        KnowledgeNode center = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Center")));
        KnowledgeNode nodeA = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "NodeA")));
        KnowledgeNode nodeB = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "NodeB")));
        KnowledgeNode nodeC = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "NodeC")));

        KnowledgeRelationship edge1 = new KnowledgeRelationship(RelationshipId.generate(), center.id(), nodeA.id(), "DEPENDS_ON", new GraphWeight(1.0));
        KnowledgeRelationship edge2 = new KnowledgeRelationship(RelationshipId.generate(), nodeA.id(), nodeB.id(), "DEPENDS_ON", new GraphWeight(1.0));
        KnowledgeRelationship edge3 = new KnowledgeRelationship(RelationshipId.generate(), nodeB.id(), nodeC.id(), "DEPENDS_ON", new GraphWeight(1.0));

        GraphTransaction tx = new GraphTransaction(List.of(center, nodeA, nodeB, nodeC), List.of(edge1, edge2, edge3));
        adapter.commit(tx);

        Collection<KnowledgeNode> subGraph1 = adapter.querySubGraph(center.id(), 1);
        assertThat(subGraph1).containsExactlyInAnyOrder(center, nodeA);

        Collection<KnowledgeNode> subGraph2 = adapter.querySubGraph(center.id(), 2);
        assertThat(subGraph2).containsExactlyInAnyOrder(center, nodeA, nodeB);
    }
}
