package com.contextengine.application.knowledge;

import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.Direction;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.RelationshipId;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.application.knowledge.exception.KnowledgeException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying the Knowledge Engine lifecycle, builder, and resolver logic.
 */
public class KnowledgeEngineTest {

    @Test
    public void testKnowledgeSessionStateTransitions() {
        KnowledgeSession session = new KnowledgeSession("session-123");
        assertEquals(KnowledgeSession.State.CREATED, session.state());

        session.transitionTo(KnowledgeSession.State.BUILDING);
        assertEquals(KnowledgeSession.State.BUILDING, session.state());

        session.transitionTo(KnowledgeSession.State.VALIDATING);
        assertEquals(KnowledgeSession.State.VALIDATING, session.state());

        session.transitionTo(KnowledgeSession.State.ACTIVE);
        assertEquals(KnowledgeSession.State.ACTIVE, session.state());

        session.transitionTo(KnowledgeSession.State.FAULTED);
        assertEquals(KnowledgeSession.State.FAULTED, session.state());

        // Cannot transition from FAULTED to ACTIVE
        assertThrows(IllegalStateException.class, () -> session.transitionTo(KnowledgeSession.State.ACTIVE));

        // Can transition from FAULTED to CREATED
        session.transitionTo(KnowledgeSession.State.CREATED);
        assertEquals(KnowledgeSession.State.CREATED, session.state());
    }

    @Test
    public void testKnowledgeContextURNAndIdentityResolution() {
        ProjectId projectId = new ProjectId(UUID.randomUUID());
        KnowledgeContext context = new KnowledgeContext(projectId, "FULL");

        assertEquals(projectId, context.projectId());
        assertEquals("FULL", context.scanMode());

        String urn1 = "urn:ce:node:myproject:file:hash1";
        String urn2 = "urn:ce:node:myproject:file:hash2";

        NodeId id1 = context.resolveNodeId(urn1);
        NodeId id1Copy = context.resolveNodeId(urn1);
        NodeId id2 = context.resolveNodeId(urn2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(id1, id1Copy);
        assertNotEquals(id1, id2);

        context.setAttribute("testKey", "testVal");
        assertEquals("testVal", context.getAttribute("testKey"));
    }

    @Test
    public void testKnowledgeGraphBuilderNodeCreation() {
        ProjectId projectId = new ProjectId(UUID.randomUUID());
        KnowledgeContext context = new KnowledgeContext(projectId, "FULL");
        KnowledgeGraph graph = new KnowledgeGraph(projectId);

        List<ScanCandidate> candidates = List.of(
            new ScanCandidate("src/App.java", "/absolute/src/App.java", 1024, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        Map<String, String> symbolMetadata = new HashMap<>();
        symbolMetadata.put("references", "HelperClass");
        List<SourceSymbol> symbols = List.of(
            new SourceSymbol("App", "CLASS", "src/App.java", 10, 50, symbolMetadata)
        );

        KnowledgeGraphBuilder builder = new KnowledgeGraphBuilder();
        builder.buildNodes(graph, candidates, symbols, context);

        // Nodes: PROJECT, FILE, CLASS
        assertEquals(3, graph.nodes().size());

        KnowledgeNode projNode = graph.nodes().stream().filter(n -> n.type().equals("PROJECT")).findFirst().orElse(null);
        assertNotNull(projNode);
        assertEquals(projectId.value().toString(), projNode.attributes().get("name"));

        KnowledgeNode fileNode = graph.nodes().stream().filter(n -> n.type().equals("FILE")).findFirst().orElse(null);
        assertNotNull(fileNode);
        assertEquals("src/App.java", fileNode.attributes().get("name"));

        KnowledgeNode classNode = graph.nodes().stream().filter(n -> n.type().equals("CLASS")).findFirst().orElse(null);
        assertNotNull(classNode);
        assertEquals("App", classNode.attributes().get("name"));
    }

    @Test
    public void testRelationshipResolution() {
        ProjectId projectId = new ProjectId(UUID.randomUUID());
        KnowledgeContext context = new KnowledgeContext(projectId, "FULL");
        KnowledgeGraph graph = new KnowledgeGraph(projectId);

        List<ScanCandidate> candidates = List.of(
            new ScanCandidate("src/App.java", "/absolute/src/App.java", 1024, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("src/Helper.java", "/absolute/src/Helper.java", 512, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        Map<String, String> symbolMetadata = new HashMap<>();
        symbolMetadata.put("references", "Helper");
        List<SourceSymbol> symbols = List.of(
            new SourceSymbol("App", "CLASS", "src/App.java", 10, 50, symbolMetadata),
            new SourceSymbol("Helper", "CLASS", "src/Helper.java", 1, 20, Collections.emptyMap())
        );

        KnowledgeGraphBuilder builder = new KnowledgeGraphBuilder();
        builder.buildNodes(graph, candidates, symbols, context);

        RelationshipResolver resolver = new RelationshipResolver();
        resolver.resolve(graph, context);

        // Relationships: 
        // 2 x FILE BELONGS_TO PROJECT
        // 2 x CLASS BELONGS_TO FILE
        // 1 x App REFERENCES Helper
        assertEquals(5, graph.relationships().size());

        // Validate references relationship exists
        KnowledgeRelationship refEdge = graph.relationships().stream()
            .filter(r -> r.type().equals("REFERENCES"))
            .findFirst()
            .orElse(null);

        assertNotNull(refEdge);
        assertEquals("REFERENCES", refEdge.type());
    }

    @Test
    public void testKnowledgeEngineLifecycleOrchestration() {
        ProjectId projectId = new ProjectId(UUID.randomUUID());
        FakeKnowledgeGraphRepository repository = new FakeKnowledgeGraphRepository();
        KnowledgeGraphBuilder builder = new KnowledgeGraphBuilder();
        RelationshipResolver resolver = new RelationshipResolver();

        KnowledgeEngine engine = new KnowledgeEngine(repository, builder, resolver);

        List<ScanCandidate> candidates = List.of(
            new ScanCandidate("src/App.java", "/absolute/src/App.java", 1024, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );
        List<SourceSymbol> symbols = List.of(
            new SourceSymbol("App", "CLASS", "src/App.java", 10, 50, Collections.emptyMap())
        );

        KnowledgeGraph result = engine.buildGraph(projectId, "FULL", candidates, symbols);

        assertNotNull(result);
        assertEquals(projectId, result.projectId());
        assertNotNull(repository.graph);
        assertEquals(result, repository.graph);
    }

    private static class FakeKnowledgeGraphRepository implements KnowledgeGraphRepository {
        private KnowledgeGraph graph;

        @Override
        public void save(KnowledgeGraph graph) {
            this.graph = graph;
        }

        @Override
        public void commit(GraphTransaction transaction) {}

        @Override
        public Optional<KnowledgeNode> findNodeById(NodeId nodeId) {
            if (graph == null) return Optional.empty();
            return graph.nodes().stream().filter(n -> n.id().equals(nodeId)).findFirst();
        }

        @Override
        public Optional<KnowledgeRelationship> findRelationshipById(RelationshipId relationshipId) {
            if (graph == null) return Optional.empty();
            return graph.relationships().stream().filter(r -> r.id().equals(relationshipId)).findFirst();
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
            if (graph == null) return Collections.emptyList();
            return graph.nodes();
        }

        @Override
        public Collection<KnowledgeRelationship> findRelationshipsByProject(ProjectId projectId) {
            if (graph == null) return Collections.emptyList();
            return graph.relationships();
        }
    }
}
