package com.contextengine.application.knowledge.context;

import com.contextengine.application.knowledge.graph.GraphNode;
import com.contextengine.application.knowledge.graph.GraphRelationship;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.graph.KnowledgeGraphConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ContextAssemblyPipelineIntegrationTest {

    @Autowired
    private ContextAssemblyEngine contextAssemblyEngine;

    @Test
    void testDeterministicSelection() {
        KnowledgeGraph graph = new KnowledgeGraph("p-sel", new KnowledgeGraphConfiguration());
        graph.addNode(new GraphNode("project:p-sel", GraphNode.Type.PROJECT, "Project Selector", null));
        graph.addNode(new GraphNode("workspace:w-sel", GraphNode.Type.WORKSPACE, "Workspace Selector", null));
        graph.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Map.of("language", "JAVA")));
        graph.addNode(new GraphNode("dep:maven-dep", GraphNode.Type.DEPENDENCY, "maven-dep", Map.of("version", "1.0")));

        graph.addRelationship(new GraphRelationship("project:p-sel", "workspace:w-sel", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-sel", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("project:p-sel", "dep:maven-dep", GraphRelationship.Type.DEPENDS_ON, null));

        // 1. Exclude Dependencies
        ContextAssemblyConfiguration configNoDeps = new ContextAssemblyConfiguration(false, true, true, true, 10);
        ContextAssemblyContext contextNoDeps = new ContextAssemblyContext(graph, configNoDeps);
        ContextAssemblyResult resultNoDeps = contextAssemblyEngine.assemble(contextNoDeps);

        // Should have 3 fragments (PROJECT, WORKSPACE, FILE)
        assertEquals(3, resultNoDeps.fragments().size());
        assertEquals(1, resultNoDeps.statistics().filteredFragments());

        // 2. Exclude Workspace Metadata
        ContextAssemblyConfiguration configNoMeta = new ContextAssemblyConfiguration(true, true, true, false, 10);
        ContextAssemblyContext contextNoMeta = new ContextAssemblyContext(graph, configNoMeta);
        ContextAssemblyResult resultNoMeta = contextAssemblyEngine.assemble(contextNoMeta);

        // Should have 2 fragments (FILE, DEPENDENCY)
        assertEquals(2, resultNoMeta.fragments().size());
        assertEquals(2, resultNoMeta.statistics().filteredFragments());
    }

    @Test
    void testHiddenAndGeneratedFileExclusion() {
        KnowledgeGraph graph = new KnowledgeGraph("p-hidden", new KnowledgeGraphConfiguration());
        graph.addNode(new GraphNode("project:p-hidden", GraphNode.Type.PROJECT, "Project Hidden", null));
        graph.addNode(new GraphNode("workspace:w-hidden", GraphNode.Type.WORKSPACE, "Workspace Hidden", null));
        
        // standard file
        graph.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Map.of("language", "JAVA")));
        // hidden file by path
        graph.addNode(new GraphNode("file:.hidden-config", GraphNode.Type.FILE, ".hidden-config", Map.of("language", "YAML")));
        // generated file by path
        graph.addNode(new GraphNode("file:target/generated-sources/Gen.java", GraphNode.Type.FILE, "Gen.java", Map.of("language", "JAVA")));
        // generated file by metadata
        graph.addNode(new GraphNode("file:src/B.java", GraphNode.Type.FILE, "B.java", Map.of("language", "JAVA", "generated", true)));

        graph.addRelationship(new GraphRelationship("project:p-hidden", "workspace:w-hidden", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-hidden", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-hidden", "file:.hidden-config", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-hidden", "file:target/generated-sources/Gen.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-hidden", "file:src/B.java", GraphRelationship.Type.CONTAINS, null));

        ContextAssemblyContext context = new ContextAssemblyContext(graph, new ContextAssemblyConfiguration());
        ContextAssemblyResult result = contextAssemblyEngine.assemble(context);

        // Should only select: PROJECT, WORKSPACE, file:src/A.java (3 fragments)
        assertEquals(3, result.fragments().size());
        assertEquals(3, result.statistics().filteredFragments());
    }

    @Test
    void testValidationRules() {
        ContextValidator validator = new ContextValidator();
        KnowledgeGraph graph = new KnowledgeGraph("p-val", new KnowledgeGraphConfiguration());
        
        graph.addNode(new GraphNode("project:p-val", GraphNode.Type.PROJECT, "Project", null));
        graph.addNode(new GraphNode("workspace:w-val", GraphNode.Type.WORKSPACE, "Workspace", null));
        graph.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Map.of("language", "JAVA")));
        graph.addNode(new GraphNode("symbol:src/A.java:foo", GraphNode.Type.SYMBOL, "foo", Map.of("filePath", "src/A.java", "kind", "METHOD")));

        graph.addRelationship(new GraphRelationship("project:p-val", "workspace:w-val", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-val", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("file:src/A.java", "symbol:src/A.java:foo", GraphRelationship.Type.DEFINES, null));

        // 1. Validation Success
        ContextAssemblyResult assemblyResult = contextAssemblyEngine.assemble(new ContextAssemblyContext(graph, new ContextAssemblyConfiguration()));
        ContextValidationResult validationResult = validator.validate(assemblyResult.fragments(), graph);
        assertTrue(validationResult.isValid());
        assertTrue(validationResult.errors().isEmpty());

        // 2. Missing Required Metadata (e.g. language missing from file, kind missing from symbol)
        KnowledgeGraph graphMissingMeta = new KnowledgeGraph("p-val", new KnowledgeGraphConfiguration());
        graphMissingMeta.addNode(new GraphNode("project:p-val", GraphNode.Type.PROJECT, "Project", null));
        graphMissingMeta.addNode(new GraphNode("workspace:w-val", GraphNode.Type.WORKSPACE, "Workspace", null));
        graphMissingMeta.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Collections.emptyMap())); // no language
        graphMissingMeta.addNode(new GraphNode("symbol:src/A.java:foo", GraphNode.Type.SYMBOL, "foo", Map.of("filePath", "src/A.java"))); // no kind

        graphMissingMeta.addRelationship(new GraphRelationship("project:p-val", "workspace:w-val", GraphRelationship.Type.CONTAINS, null));
        graphMissingMeta.addRelationship(new GraphRelationship("workspace:w-val", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graphMissingMeta.addRelationship(new GraphRelationship("file:src/A.java", "symbol:src/A.java:foo", GraphRelationship.Type.DEFINES, null));

        ContextAssemblyResult assemblyResult2 = contextAssemblyEngine.assemble(new ContextAssemblyContext(graphMissingMeta, new ContextAssemblyConfiguration()));
        ContextValidationResult valResultMissingMeta = validator.validate(assemblyResult2.fragments(), graphMissingMeta);
        assertFalse(valResultMissingMeta.isValid());
        assertEquals(2, valResultMissingMeta.errors().size());
        assertTrue(valResultMissingMeta.errors().get(0).contains("language"));
        assertTrue(valResultMissingMeta.errors().get(1).contains("kind"));

        // 3. Invalid Fragment Ordering (e.g. child fragment placed before parent)
        List<ContextFragment> orderedFragments = assemblyResult.fragments();
        List<ContextFragment> reversedFragments = new ArrayList<>(orderedFragments);
        Collections.reverse(reversedFragments); // places child symbol before file, and file before workspace

        ContextValidationResult valResultOrdering = validator.validate(reversedFragments, graph);
        assertFalse(valResultOrdering.isValid());
        assertTrue(valResultOrdering.errors().stream().anyMatch(e -> e.contains("Invalid fragment ordering")));

        // 4. Orphan Fragment (disconnected node in graph, but present as fragment)
        KnowledgeGraph graphWithOrphan = new KnowledgeGraph("p-val", new KnowledgeGraphConfiguration());
        graphWithOrphan.addNode(new GraphNode("project:p-val", GraphNode.Type.PROJECT, "Project", null));
        graphWithOrphan.addNode(new GraphNode("workspace:w-val", GraphNode.Type.WORKSPACE, "Workspace", null));
        graphWithOrphan.addNode(new GraphNode("file:src/Orphan.java", GraphNode.Type.FILE, "Orphan.java", Map.of("language", "JAVA")));

        graphWithOrphan.addRelationship(new GraphRelationship("project:p-val", "workspace:w-val", GraphRelationship.Type.CONTAINS, null));
        // file:src/Orphan.java is not connected to workspace:w-val

        ContextAssemblyResult assemblyResultOrphan = contextAssemblyEngine.assemble(new ContextAssemblyContext(graphWithOrphan, new ContextAssemblyConfiguration()));
        ContextValidationResult valResultOrphan = validator.validate(assemblyResultOrphan.fragments(), graphWithOrphan);
        assertFalse(valResultOrphan.isValid());
        assertTrue(valResultOrphan.errors().stream().anyMatch(e -> e.contains("Orphan fragment detected")));
    }

    @Test
    void testIncrementalContextUpdates() {
        IncrementalContextAssembler assembler = new IncrementalContextAssembler();
        assembler.clearCache("p-inc");

        // 1. Initial run setup
        KnowledgeGraph graph1 = new KnowledgeGraph("p-inc", new KnowledgeGraphConfiguration());
        graph1.addNode(new GraphNode("project:p-inc", GraphNode.Type.PROJECT, "Project", null));
        graph1.addNode(new GraphNode("workspace:w-inc", GraphNode.Type.WORKSPACE, "Workspace", null));
        graph1.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Map.of("language", "JAVA")));
        graph1.addNode(new GraphNode("file:src/B.java", GraphNode.Type.FILE, "B.java", Map.of("language", "JAVA")));

        graph1.addRelationship(new GraphRelationship("project:p-inc", "workspace:w-inc", GraphRelationship.Type.CONTAINS, null));
        graph1.addRelationship(new GraphRelationship("workspace:w-inc", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graph1.addRelationship(new GraphRelationship("workspace:w-inc", "file:src/B.java", GraphRelationship.Type.CONTAINS, null));

        ContextAssemblyContext context1 = new ContextAssemblyContext(
            graph1,
            new ContextAssemblyConfiguration(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            false,
            "hash-1"
        );

        ContextAssemblyResult result1 = contextAssemblyEngine.assemble(context1);
        assertEquals(4, result1.fragments().size());
        assertEquals(4, result1.statistics().totalFragments());

        // 2. Unchanged Context Reuse (matching hash)
        ContextAssemblyContext contextUnchanged = new ContextAssemblyContext(
            graph1,
            new ContextAssemblyConfiguration(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            false,
            "hash-1"
        );
        ContextAssemblyResult resultUnchanged = contextAssemblyEngine.assemble(contextUnchanged);
        // Should fetch exactly the same result reference from cache
        assertSame(result1.fragments(), resultUnchanged.fragments());

        // 3. Partial Rebuild (Incremental context update where src/B.java changes, src/A.java is reused)
        KnowledgeGraph graph2 = new KnowledgeGraph("p-inc", new KnowledgeGraphConfiguration());
        graph2.addNode(new GraphNode("project:p-inc", GraphNode.Type.PROJECT, "Project", null));
        graph2.addNode(new GraphNode("workspace:w-inc", GraphNode.Type.WORKSPACE, "Workspace", null));
        graph2.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Map.of("language", "JAVA")));
        // B.java modified (say content is rebuilt, label changes to "B.java MODIFIED")
        graph2.addNode(new GraphNode("file:src/B.java", GraphNode.Type.FILE, "B.java MODIFIED", Map.of("language", "JAVA")));

        graph2.addRelationship(new GraphRelationship("project:p-inc", "workspace:w-inc", GraphRelationship.Type.CONTAINS, null));
        graph2.addRelationship(new GraphRelationship("workspace:w-inc", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graph2.addRelationship(new GraphRelationship("workspace:w-inc", "file:src/B.java", GraphRelationship.Type.CONTAINS, null));

        ContextAssemblyContext contextIncremental = new ContextAssemblyContext(
            graph2,
            new ContextAssemblyConfiguration(),
            Collections.emptyList(),
            List.of("src/B.java"), // modified
            Collections.emptyList(),
            true, // isIncremental
            "hash-2"
        );

        ContextAssemblyResult resultIncremental = contextAssemblyEngine.assemble(contextIncremental);

        assertEquals(4, resultIncremental.fragments().size());
        
        // Assert B.java was rebuilt (new content/title), but A.java fragment reference was completely reused!
        ContextFragment fragA1 = result1.fragments().stream().filter(f -> f.sourceNodeId().equals("file:src/A.java")).findFirst().orElseThrow();
        ContextFragment fragA2 = resultIncremental.fragments().stream().filter(f -> f.sourceNodeId().equals("file:src/A.java")).findFirst().orElseThrow();
        assertSame(fragA1, fragA2, "Fragment A.java must be exactly reused from previous result cache");

        ContextFragment fragB1 = result1.fragments().stream().filter(f -> f.sourceNodeId().equals("file:src/B.java")).findFirst().orElseThrow();
        ContextFragment fragB2 = resultIncremental.fragments().stream().filter(f -> f.sourceNodeId().equals("file:src/B.java")).findFirst().orElseThrow();
        assertNotSame(fragB1, fragB2, "Fragment B.java must be rebuilt and have a new reference");
        assertEquals("B.java MODIFIED", fragB2.title());
    }

    @Test
    void testStatisticsTracking() {
        KnowledgeGraph graph = new KnowledgeGraph("p-stats", new KnowledgeGraphConfiguration());
        graph.addNode(new GraphNode("project:p-stats", GraphNode.Type.PROJECT, "Project", null));
        graph.addNode(new GraphNode("workspace:w-stats", GraphNode.Type.WORKSPACE, "Workspace", null));
        // 2 standard files
        graph.addNode(new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", Map.of("language", "JAVA")));
        graph.addNode(new GraphNode("file:src/B.java", GraphNode.Type.FILE, "B.java", Map.of("language", "JAVA")));
        // 1 hidden file (should be filtered)
        graph.addNode(new GraphNode("file:.env", GraphNode.Type.FILE, ".env", Map.of("language", "PROPERTIES")));

        graph.addRelationship(new GraphRelationship("project:p-stats", "workspace:w-stats", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-stats", "file:src/A.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-stats", "file:src/B.java", GraphRelationship.Type.CONTAINS, null));
        graph.addRelationship(new GraphRelationship("workspace:w-stats", "file:.env", GraphRelationship.Type.CONTAINS, null));

        ContextAssemblyContext context = new ContextAssemblyContext(graph, new ContextAssemblyConfiguration());
        ContextAssemblyResult result = contextAssemblyEngine.assemble(context);

        ContextStatistics stats = result.statistics();
        assertEquals(5, stats.totalFragments(), "Created total 5 fragments");
        assertEquals(4, stats.selectedFragments(), "Selected 4 fragments (PROJECT, WORKSPACE, A, B)");
        assertEquals(1, stats.filteredFragments(), "Filtered 1 fragment (.env)");
        assertEquals(0, stats.duplicateFragmentsRemoved());
        assertEquals(0, stats.validationWarnings());
        assertTrue(stats.assemblyDuration() >= 0);
        assertTrue(stats.validationDuration() >= 0);
    }
}
