package com.contextengine.application.knowledge.graph;

import com.contextengine.application.knowledge.engine.KnowledgeEngineConfiguration;
import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import com.contextengine.application.knowledge.engine.KnowledgeEngineImpl;
import com.contextengine.application.knowledge.engine.KnowledgeEngineResult;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeGraphIntegrationTest {

    private GraphValidator validator;
    private KnowledgeEngineImpl knowledgeEngine;
    private GraphUpdateEngine updateEngine;
    private KnowledgeGraphBuilder graphBuilder;

    @BeforeEach
    void setUp() {
        validator = new GraphValidator();
        knowledgeEngine = new KnowledgeEngineImpl();
        updateEngine = new GraphUpdateEngine();
        graphBuilder = new KnowledgeGraphBuilder(updateEngine);
        updateEngine.clearCache("proj-1");
    }

    @Test
    void testEmptyGraphValidation() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        nodeBuilder.createOrGetNode("project:proj-1", GraphNode.Type.PROJECT, "Project", null);
        nodeBuilder.createOrGetNode("workspace:work-1", GraphNode.Type.WORKSPACE, "Workspace", null);
        relBuilder.createOrGetRelationship("project:proj-1", "workspace:work-1", GraphRelationship.Type.CONTAINS, null);

        GraphValidationResult result = validator.validate(graph);
        assertTrue(result.isValid(), "Empty graph with only Project and Workspace should be valid");
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void testMultiNodeCyclicContains() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration(true, true));
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        nodeBuilder.createOrGetNode("dir-a", GraphNode.Type.DIRECTORY, "a", null);
        nodeBuilder.createOrGetNode("dir-b", GraphNode.Type.DIRECTORY, "b", null);
        nodeBuilder.createOrGetNode("dir-c", GraphNode.Type.DIRECTORY, "c", null);

        relBuilder.createOrGetRelationship("dir-a", "dir-b", GraphRelationship.Type.CONTAINS, null);
        relBuilder.createOrGetRelationship("dir-b", "dir-c", GraphRelationship.Type.CONTAINS, null);
        relBuilder.createOrGetRelationship("dir-c", "dir-a", GraphRelationship.Type.CONTAINS, null);

        GraphValidationResult result = validator.validate(graph);
        assertFalse(result.isValid(), "Multi-node cycle in CONTAINS relationships should be invalid");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Circular CONTAINS relationship")));
    }

    @Test
    void testMissingParentContains() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        nodeBuilder.createOrGetNode("project:proj-1", GraphNode.Type.PROJECT, "Project", null);
        nodeBuilder.createOrGetNode("workspace:work-1", GraphNode.Type.WORKSPACE, "Workspace", null);
        relBuilder.createOrGetRelationship("project:proj-1", "workspace:work-1", GraphRelationship.Type.CONTAINS, null);

        // Add file node but do not link it to workspace/directory via CONTAINS
        nodeBuilder.createOrGetNode("file:src/Orphan.java", GraphNode.Type.FILE, "Orphan.java", new HashMap<>());

        GraphValidationResult result = validator.validate(graph);
        assertFalse(result.isValid(), "File without parent CONTAINS should be invalid");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Node lacks a parent CONTAINS relationship")));
    }

    @Test
    void testOrphanDirectoriesAndSymbols() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        nodeBuilder.createOrGetNode("project:proj-1", GraphNode.Type.PROJECT, "Project", null);
        nodeBuilder.createOrGetNode("workspace:work-1", GraphNode.Type.WORKSPACE, "Workspace", null);
        relBuilder.createOrGetRelationship("project:proj-1", "workspace:work-1", GraphRelationship.Type.CONTAINS, null);

        // Directory node created but not connected to anything
        nodeBuilder.createOrGetNode("dir:orphandir", GraphNode.Type.DIRECTORY, "orphandir", new HashMap<>());

        GraphValidationResult result = validator.validate(graph);
        assertFalse(result.isValid(), "Orphan directory should be invalid");
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Orphan node detected") || e.contains("Node lacks a parent CONTAINS relationship")));
    }

    @Test
    void testMultiErrorDetection() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration(false, true));
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        // Error 1: self loop on project node
        nodeBuilder.createOrGetNode("project:proj-1", GraphNode.Type.PROJECT, "Project", null);
        relBuilder.createOrGetRelationship("project:proj-1", "project:proj-1", GraphRelationship.Type.REFERENCES, null);

        // Error 2: orphan file
        nodeBuilder.createOrGetNode("file:src/A.java", GraphNode.Type.FILE, "A.java", new HashMap<>());

        GraphValidationResult result = validator.validate(graph);
        assertFalse(result.isValid());
        assertTrue(result.errors().size() >= 2, "Should capture multiple validation errors");
    }

    @Test
    void testEngineIntegrationStrictVsLax() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("filesCount", 0);
        stats.put("symbolsCount", 0);

        KnowledgeEngineContext contextStrict = new KnowledgeEngineContext(
            "p1", "w1", "s1", "", Instant.now(), stats,
            new KnowledgeEngineConfiguration(5, true, true, true, "STRICT")
        );
        KnowledgeEngineResult resultStrict = knowledgeEngine.process(contextStrict);
        assertEquals("FAILED", resultStrict.processingStatus(), "Strict mode must fail on empty structural hash");

        KnowledgeEngineContext contextLax = new KnowledgeEngineContext(
            "p1", "w1", "s1", "", Instant.now(), stats,
            new KnowledgeEngineConfiguration(5, true, true, true, "LAX")
        );
        KnowledgeEngineResult resultLax = knowledgeEngine.process(contextLax);
        assertEquals("COMPLETED", resultLax.processingStatus(), "Lax mode should warn but complete on empty structural hash");
        assertEquals(1, resultLax.statistics().warnings());
    }

    @Test
    void testGraphUpdateEngineConcurrentAccess() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                String pId = "proj-" + index;
                KnowledgeGraph g = new KnowledgeGraph(pId, new KnowledgeGraphConfiguration());
                updateEngine.cacheGraph(pId, g, "hash-" + index);
                
                KnowledgeGraph cached = updateEngine.getCachedGraph(pId);
                assertNotNull(cached);
                assertEquals(pId, cached.projectId());
                assertTrue(updateEngine.isUnchanged(pId, "hash-" + index));
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        for (Future<?> future : futures) {
            future.get(); // Throws exception if assert fails inside thread
        }
    }

    @Test
    void testTransitivePruning() {
        KnowledgeGraph graph = new KnowledgeGraph("proj-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        // Project and workspace
        nodeBuilder.createOrGetNode("project:proj-1", GraphNode.Type.PROJECT, "Project", null);
        nodeBuilder.createOrGetNode("workspace:work-1", GraphNode.Type.WORKSPACE, "Workspace", null);
        relBuilder.createOrGetRelationship("project:proj-1", "workspace:work-1", GraphRelationship.Type.CONTAINS, null);

        // Directories, Files, Symbols
        nodeBuilder.createOrGetNode("dir:src", GraphNode.Type.DIRECTORY, "src", Map.of("relativePath", "src"));
        nodeBuilder.createOrGetNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Map.of("absolutePath", "/root/src/A.java"));
        nodeBuilder.createOrGetNode("symbol:src/A.java:methodA", GraphNode.Type.SYMBOL, "methodA", Map.of("filePath", "src/A.java"));

        relBuilder.createOrGetRelationship("workspace:work-1", "dir:src", GraphRelationship.Type.CONTAINS, null);
        relBuilder.createOrGetRelationship("dir:src", "file:src/A.java", GraphRelationship.Type.CONTAINS, null);
        relBuilder.createOrGetRelationship("file:src/A.java", "symbol:src/A.java:methodA", GraphRelationship.Type.DEFINES, null);

        // Pruning dirty file "src/A.java"
        KnowledgeGraph pruned = updateEngine.prune(graph, List.of("src/A.java"));

        assertNull(pruned.getNode("file:src/A.java"), "Dirty file should be pruned");
        assertNull(pruned.getNode("symbol:src/A.java:methodA"), "Symbol of dirty file should be pruned");
        assertNotNull(pruned.getNode("dir:src"), "Unaffected directory should remain");
        assertNotNull(pruned.getNode("workspace:work-1"));
    }
}
