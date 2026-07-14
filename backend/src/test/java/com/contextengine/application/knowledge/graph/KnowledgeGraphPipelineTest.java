package com.contextengine.application.knowledge.graph;

import com.contextengine.application.knowledge.engine.KnowledgeEngineConfiguration;
import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeGraphPipelineTest {

    private GraphUpdateEngine updateEngine;
    private KnowledgeGraphBuilder graphBuilder;
    private GraphValidator validator;

    @BeforeEach
    void setUp() {
        updateEngine = new GraphUpdateEngine();
        graphBuilder = new KnowledgeGraphBuilder(updateEngine);
        validator = new GraphValidator();
        updateEngine.clearCache("proj-1");
    }

    @Test
    void testDeterministicGraphGeneration() {
        List<ScanCandidate> candidates = List.of(
            new ScanCandidate("src/B.java", "/root/src/B.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("src/A.java", "/root/src/A.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        List<SourceSymbol> symbols = List.of(
            new SourceSymbol("methodB", "METHOD", "src/B.java", 5, 10, Collections.emptyMap()),
            new SourceSymbol("methodA", "METHOD", "src/A.java", 1, 4, Collections.emptyMap())
        );

        List<ProjectDependency> deps = List.of(
            new ProjectDependency("dep-B", "2.0.0", "MAVEN", "COMPILE"),
            new ProjectDependency("dep-A", "1.0.0", "MAVEN", "COMPILE")
        );

        // Run 1: Original Order
        KnowledgeEngineContext context1 = new KnowledgeEngineContext(
            "proj-1", "work-1", "scan-1", "hash-1", Instant.now(), new HashMap<>(),
            new KnowledgeEngineConfiguration(), candidates, symbols, deps,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeGraph graph1 = graphBuilder.build(context1);

        // Run 2: Shuffled Collections
        List<ScanCandidate> shuffledCandidates = new ArrayList<>(candidates);
        Collections.reverse(shuffledCandidates);

        List<SourceSymbol> shuffledSymbols = new ArrayList<>(symbols);
        Collections.reverse(shuffledSymbols);

        List<ProjectDependency> shuffledDeps = new ArrayList<>(deps);
        Collections.reverse(shuffledDeps);

        updateEngine.clearCache("proj-1"); // Clear cache to force rebuild

        KnowledgeEngineContext context2 = new KnowledgeEngineContext(
            "proj-1", "work-1", "scan-1", "hash-1", Instant.now(), new HashMap<>(),
            new KnowledgeEngineConfiguration(), shuffledCandidates, shuffledSymbols, shuffledDeps,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeGraph graph2 = graphBuilder.build(context2);

        // Assert determinism: node & relationship sizes must match and ordering must produce identical sets
        assertEquals(graph1.nodes().size(), graph2.nodes().size());
        assertEquals(graph1.relationships().size(), graph2.relationships().size());

        for (GraphNode node : graph1.nodes()) {
            assertNotNull(graph2.getNode(node.id()));
            assertEquals(node.type(), graph2.getNode(node.id()).type());
        }
    }

    @Test
    void testGraphValidationSuccess() {
        List<ScanCandidate> candidates = List.of(
            new ScanCandidate("src/A.java", "/root/src/A.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );
        List<SourceSymbol> symbols = List.of(
            new SourceSymbol("methodA", "METHOD", "src/A.java", 1, 4, Collections.emptyMap())
        );

        KnowledgeEngineContext context = new KnowledgeEngineContext(
            "proj-1", "work-1", "scan-1", "hash-1", Instant.now(), new HashMap<>(),
            new KnowledgeEngineConfiguration(), candidates, symbols, Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeGraph graph = graphBuilder.build(context);
        GraphValidationResult result = validator.validate(graph);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void testGraphValidationFailures() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration(false, true));
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        // 1. Invalid self loop
        nodeBuilder.createOrGetNode("file-1", GraphNode.Type.FILE, "A.java", Collections.emptyMap());
        relBuilder.createOrGetRelationship("file-1", "file-1", GraphRelationship.Type.REFERENCES, Collections.emptyMap());

        GraphValidationResult resultSelfLoop = validator.validate(graph);
        assertFalse(resultSelfLoop.isValid());
        assertTrue(resultSelfLoop.errors().stream().anyMatch(e -> e.contains("Forbidden self loop")));

        // 2. Orphan Node (added disconnected node)
        KnowledgeGraph graphOrphan = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilderOrphan = new KnowledgeNodeBuilder(graphOrphan);
        KnowledgeRelationshipBuilder relBuilderOrphan = new KnowledgeRelationshipBuilder(graphOrphan);

        nodeBuilderOrphan.createOrGetNode("project:proj-1", GraphNode.Type.PROJECT, "Project", Collections.emptyMap());
        nodeBuilderOrphan.createOrGetNode("workspace:work-1", GraphNode.Type.WORKSPACE, "Workspace", Collections.emptyMap());
        relBuilderOrphan.createOrGetRelationship("project:proj-1", "workspace:work-1", GraphRelationship.Type.CONTAINS, Collections.emptyMap());
        
        nodeBuilderOrphan.createOrGetNode("file-orphan", GraphNode.Type.FILE, "Orphan.java", Collections.emptyMap());

        GraphValidationResult resultOrphan = validator.validate(graphOrphan);
        assertFalse(resultOrphan.isValid());
        assertTrue(resultOrphan.errors().stream().anyMatch(e -> e.contains("Orphan node detected")));
    }

    @Test
    void testCircularContainmentDetection() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration(true, true));
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        nodeBuilder.createOrGetNode("dir-a", GraphNode.Type.DIRECTORY, "a", Collections.emptyMap());
        nodeBuilder.createOrGetNode("dir-b", GraphNode.Type.DIRECTORY, "b", Collections.emptyMap());

        relBuilder.createOrGetRelationship("dir-a", "dir-b", GraphRelationship.Type.CONTAINS, Collections.emptyMap());
        relBuilder.createOrGetRelationship("dir-b", "dir-a", GraphRelationship.Type.CONTAINS, Collections.emptyMap());

        GraphValidationResult result = validator.validate(graph);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Circular CONTAINS relationship")));
    }

    @Test
    void testUnchangedGraphReuse() {
        List<ScanCandidate> candidates = List.of(
            new ScanCandidate("src/A.java", "/root/src/A.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        KnowledgeEngineContext context1 = new KnowledgeEngineContext(
            "proj-1", "work-1", "scan-1", "hash-same", Instant.now(), new HashMap<>(),
            new KnowledgeEngineConfiguration(), candidates, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeGraph graph1 = graphBuilder.build(context1);

        // Request again with same hash
        KnowledgeEngineContext context2 = new KnowledgeEngineContext(
            "proj-1", "work-1", "scan-2", "hash-same", Instant.now(), new HashMap<>(),
            new KnowledgeEngineConfiguration(), candidates, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeGraph graph2 = graphBuilder.build(context2);
        assertSame(graph1, graph2); // Graph object should be reused directly
    }

    @Test
    void testIncrementalReuseAndPartialRebuild() {
        List<ScanCandidate> candidates1 = List.of(
            new ScanCandidate("src/A.java", "/root/src/A.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("src/B.java", "/root/src/B.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        KnowledgeEngineContext context1 = new KnowledgeEngineContext(
            "proj-1", "work-1", "scan-1", "hash-1", Instant.now(), new HashMap<>(),
            new KnowledgeEngineConfiguration(), candidates1, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeGraph graph1 = graphBuilder.build(context1);
        assertNotNull(graph1.getNode("file:src/A.java"));
        assertNotNull(graph1.getNode("file:src/B.java"));

        // Incremental Scan: B.java deleted, C.java added
        List<ScanCandidate> candidates2 = List.of(
            new ScanCandidate("src/A.java", "/root/src/A.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("src/C.java", "/root/src/C.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        KnowledgeEngineContext context2 = new KnowledgeEngineContext(
            "proj-1", "work-1", "scan-2", "hash-2", Instant.now(), new HashMap<>(),
            new KnowledgeEngineConfiguration(), candidates2, Collections.emptyList(), Collections.emptyList(),
            List.of("src/C.java"), Collections.emptyList(), List.of("src/B.java"), true
        );

        KnowledgeGraph graph2 = graphBuilder.build(context2);
        assertNotSame(graph1, graph2);

        // Verify C.java is present, B.java is pruned, A.java is reused
        assertNotNull(graph2.getNode("file:src/A.java"));
        assertNotNull(graph2.getNode("file:src/C.java"));
        assertNull(graph2.getNode("file:src/B.java"));
    }
}
