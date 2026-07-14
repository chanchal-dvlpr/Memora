package com.contextengine.application.knowledge.context;

import com.contextengine.application.knowledge.graph.GraphNode;
import com.contextengine.application.knowledge.graph.GraphRelationship;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.graph.KnowledgeGraphConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ContextAssemblyEngineTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ContextAssemblyEngine contextAssemblyEngine;

    @Test
    void testSpringBeanRegistration() {
        assertNotNull(applicationContext, "Spring ApplicationContext must not be null");
        assertNotNull(contextAssemblyEngine, "ContextAssemblyEngine bean must be registered and injected");

        ContextAssemblyEngine retrieved = applicationContext.getBean(ContextAssemblyEngine.class);
        assertSame(contextAssemblyEngine, retrieved);
    }

    @Test
    void testConfigurationDefaults() {
        ContextAssemblyConfiguration config = new ContextAssemblyConfiguration();
        assertTrue(config.includeDependencies());
        assertTrue(config.includeDirectories());
        assertTrue(config.includeSymbols());
        assertTrue(config.includeWorkspaceMetadata());
        assertEquals(10, config.maximumAssemblyDepth());
    }

    @Test
    void testEmptyGraphAssembly() {
        KnowledgeGraph graph = new KnowledgeGraph("p-empty", new KnowledgeGraphConfiguration());
        // Only Project and Workspace nodes
        graph.addNode(new GraphNode("project:p-empty", GraphNode.Type.PROJECT, "Empty Project", null));
        graph.addNode(new GraphNode("workspace:w-empty", GraphNode.Type.WORKSPACE, "Empty Workspace", null));
        graph.addRelationship(new GraphRelationship("project:p-empty", "workspace:w-empty", GraphRelationship.Type.CONTAINS, null));

        // 1. With Workspace Metadata
        ContextAssemblyContext contextWithMetadata = new ContextAssemblyContext(graph, new ContextAssemblyConfiguration());
        ContextAssemblyResult resultWithMetadata = contextAssemblyEngine.assemble(contextWithMetadata);

        assertEquals("p-empty", resultWithMetadata.projectId());
        assertEquals(2, resultWithMetadata.fragments().size());
        assertEquals(ContextFragmentType.PROJECT, resultWithMetadata.fragments().get(0).fragmentType());
        assertEquals(ContextFragmentType.WORKSPACE, resultWithMetadata.fragments().get(1).fragmentType());
        assertEquals(2, resultWithMetadata.statistics().fragmentsCreated());

        // 2. Without Workspace Metadata
        ContextAssemblyConfiguration configNoMetadata = new ContextAssemblyConfiguration(true, true, true, false, 10);
        ContextAssemblyContext contextNoMetadata = new ContextAssemblyContext(graph, configNoMetadata);
        ContextAssemblyResult resultNoMetadata = contextAssemblyEngine.assemble(contextNoMetadata);

        assertTrue(resultNoMetadata.fragments().isEmpty());
        assertEquals(2, resultNoMetadata.statistics().fragmentsCreated());
    }

    @Test
    void testSingleProjectAssembly() {
        KnowledgeGraph graph = new KnowledgeGraph("p-single", new KnowledgeGraphConfiguration());
        graph.addNode(new GraphNode("project:p-single", GraphNode.Type.PROJECT, "Project 1", null));
        graph.addNode(new GraphNode("workspace:w-single", GraphNode.Type.WORKSPACE, "Workspace 1", null));
        graph.addNode(new GraphNode("dir:src", GraphNode.Type.DIRECTORY, "src", Map.of("relativePath", "src")));
        graph.addNode(new GraphNode("file:src/App.java", GraphNode.Type.FILE, "App.java", Map.of("language", "JAVA")));
        graph.addNode(new GraphNode("symbol:src/App.java:run", GraphNode.Type.SYMBOL, "run()", Map.of("filePath", "src/App.java", "kind", "METHOD")));
        graph.addNode(new GraphNode("dep:junit", GraphNode.Type.DEPENDENCY, "junit", Map.of("version", "5.10.0")));

        graph.addRelationship(new GraphRelationship("project:p-single", "workspace:w-single", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-single", "dir:src", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("dir:src", "file:src/App.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("file:src/App.java", "symbol:src/App.java:run", GraphRelationship.Type.DEFINES, null));
        graph.addRelationship(new GraphRelationship("project:p-single", "dep:junit", GraphRelationship.Type.DEPENDS_ON, null));

        ContextAssemblyContext context = new ContextAssemblyContext(graph, new ContextAssemblyConfiguration());
        ContextAssemblyResult result = contextAssemblyEngine.assemble(context);

        assertEquals(6, result.fragments().size(), "Should create 6 fragments corresponding to all 6 nodes");
        assertEquals(6, result.statistics().fragmentsCreated());

        // Assert hierarchy ordering based on deterministic target ID sorting (dep:junit is visited before workspace:w-single)
        assertEquals(ContextFragmentType.PROJECT, result.fragments().get(0).fragmentType());
        assertEquals(ContextFragmentType.DEPENDENCY, result.fragments().get(1).fragmentType());
        assertEquals(ContextFragmentType.WORKSPACE, result.fragments().get(2).fragmentType());
        assertEquals(ContextFragmentType.DIRECTORY, result.fragments().get(3).fragmentType());
        assertEquals(ContextFragmentType.FILE, result.fragments().get(4).fragmentType());
        assertEquals(ContextFragmentType.SYMBOL, result.fragments().get(5).fragmentType());
    }

    @Test
    void testMultiModuleGraphAssembly() {
        KnowledgeGraph graph = new KnowledgeGraph("p-multi", new KnowledgeGraphConfiguration());
        graph.addNode(new GraphNode("project:p-multi", GraphNode.Type.PROJECT, "Multi Project", null));
        graph.addNode(new GraphNode("workspace:w-multi", GraphNode.Type.WORKSPACE, "Workspace Multi", null));
        
        // Modules
        graph.addNode(new GraphNode("module:api", GraphNode.Type.MODULE, "API Module", null));
        graph.addNode(new GraphNode("module:core", GraphNode.Type.MODULE, "Core Module", null));

        // Connect Project -> Workspace -> Modules
        graph.addRelationship(new GraphRelationship("project:p-multi", "workspace:w-multi", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-multi", "module:api", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-multi", "module:core", GraphRelationship.Type.CONTAINS, null));

        ContextAssemblyContext context = new ContextAssemblyContext(graph, new ContextAssemblyConfiguration());
        ContextAssemblyResult result = contextAssemblyEngine.assemble(context);

        // Verification of ordering (sorted child relations by targetNodeId):
        // module:api must be visited before module:core because of sorting: "module:api".compareTo("module:core") < 0
        assertEquals(4, result.fragments().size());
        assertEquals("API Module", result.fragments().get(2).title());
        assertEquals("Core Module", result.fragments().get(3).title());
    }

    @Test
    void testDuplicateFragmentPreventionAndStatistics() {
        KnowledgeGraph graph = new KnowledgeGraph("p-duplicate", new KnowledgeGraphConfiguration());
        graph.addNode(new GraphNode("project:p-duplicate", GraphNode.Type.PROJECT, "Project Duplicate", null));
        graph.addNode(new GraphNode("workspace:w-duplicate", GraphNode.Type.WORKSPACE, "Workspace Duplicate", null));
        graph.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", null));

        // Form a graph structure where file:src/A.java is connected via two containment paths (which shouldn't happen but validates duplication guard)
        graph.addRelationship(new GraphRelationship("project:p-duplicate", "workspace:w-duplicate", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("project:p-duplicate", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-duplicate", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));

        ContextAssemblyContext context = new ContextAssemblyContext(graph, new ContextAssemblyConfiguration());
        ContextAssemblyResult result = contextAssemblyEngine.assemble(context);

        // We have 3 nodes in total. Even with multiple paths, only 3 fragments should be produced
        assertEquals(3, result.fragments().size());
        assertEquals(3, result.statistics().fragmentsCreated());
    }

    @Test
    void testDeterministicFragmentOrdering() {
        // Build graph with shuffled connection registration to verify traversal sorting is strictly deterministic
        List<GraphNode> nodes = List.of(
            new GraphNode("project:p-det", GraphNode.Type.PROJECT, "Project", null),
            new GraphNode("workspace:w-det", GraphNode.Type.WORKSPACE, "Workspace", null),
            new GraphNode("file:src/B.java", GraphNode.Type.FILE, "B.java", null),
            new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", null)
        );

        List<GraphRelationship> relationships1 = List.of(
            new GraphRelationship("project:p-det", "workspace:w-det", GraphRelationship.Type.CONTAINS, null),
            new GraphRelationship("workspace:w-det", "file:src/B.java", GraphRelationship.Type.CONTAINS, null),
            new GraphRelationship("workspace:w-det", "file:src/A.java", GraphRelationship.Type.CONTAINS, null)
        );

        // Run 1
        KnowledgeGraph graph1 = new KnowledgeGraph("p-det", new KnowledgeGraphConfiguration());
        nodes.forEach(graph1::addNode);
        relationships1.forEach(graph1::addRelationship);

        ContextAssemblyResult result1 = contextAssemblyEngine.assemble(new ContextAssemblyContext(graph1, new ContextAssemblyConfiguration()));

        // Run 2: opposite relationship registration order
        KnowledgeGraph graph2 = new KnowledgeGraph("p-det", new KnowledgeGraphConfiguration());
        nodes.forEach(graph2::addNode);
        
        List<GraphRelationship> relationships2 = new ArrayList<>(relationships1);
        Collections.reverse(relationships2);
        relationships2.forEach(graph2::addRelationship);

        ContextAssemblyResult result2 = contextAssemblyEngine.assemble(new ContextAssemblyContext(graph2, new ContextAssemblyConfiguration()));

        // Both ordered fragment outputs must be identical
        assertEquals(result1.fragments().size(), result2.fragments().size());
        for (int i = 0; i < result1.fragments().size(); i++) {
            assertEquals(result1.fragments().get(i).fragmentId(), result2.fragments().get(i).fragmentId());
            assertEquals(result1.fragments().get(i).title(), result2.fragments().get(i).title());
        }
    }
}
