package com.contextengine.application.knowledge.graph;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeGraphTest {

    @Test
    void testGraphInitializationAndConfiguration() {
        KnowledgeGraphConfiguration config = new KnowledgeGraphConfiguration();
        assertFalse(config.allowSelfLoops());
        assertTrue(config.validateNodeTypes());

        KnowledgeGraph graph = new KnowledgeGraph("project-1", config);
        assertEquals("project-1", graph.projectId());
        assertSame(config, graph.configuration());
        assertEquals(0, graph.statistics().nodeCount());
        assertEquals(0, graph.statistics().relationshipCount());
        assertTrue(graph.nodes().isEmpty());
        assertTrue(graph.relationships().isEmpty());
    }

    @Test
    void testNodeCreationAndDuplicatePrevention() {
        KnowledgeGraph graph = new KnowledgeGraph("project-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);

        Map<String, Object> props = new HashMap<>();
        props.put("path", "/root/src");

        GraphNode node1 = nodeBuilder.createOrGetNode("dir-src", GraphNode.Type.DIRECTORY, "src", props);
        assertNotNull(node1);
        assertEquals("dir-src", node1.id());
        assertEquals(GraphNode.Type.DIRECTORY, node1.type());
        assertEquals("src", node1.label());
        assertEquals("/root/src", node1.properties().get("path"));

        assertEquals(1, graph.statistics().nodeCount());
        assertEquals(1, graph.nodes().size());

        // Try creating duplicate node
        GraphNode node2 = nodeBuilder.createOrGetNode("dir-src", GraphNode.Type.DIRECTORY, "src-other", Collections.emptyMap());
        assertSame(node1, node2); // must return the existing node instance
        assertEquals(1, graph.statistics().nodeCount()); // count must remain 1
        assertEquals(1, graph.nodes().size());
    }

    @Test
    void testRelationshipCreationAndDuplicatePrevention() {
        KnowledgeGraph graph = new KnowledgeGraph("project-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        GraphNode fileNode = nodeBuilder.createOrGetNode("file-app", GraphNode.Type.FILE, "App.java", Collections.emptyMap());
        GraphNode symbolNode = nodeBuilder.createOrGetNode("sym-main", GraphNode.Type.SYMBOL, "main()", Collections.emptyMap());

        GraphRelationship rel1 = relBuilder.createOrGetRelationship(
            "file-app",
            "sym-main",
            GraphRelationship.Type.DEFINES,
            Collections.singletonMap("line", 10)
        );

        assertNotNull(rel1);
        assertEquals("file-app", rel1.sourceNodeId());
        assertEquals("sym-main", rel1.targetNodeId());
        assertEquals(GraphRelationship.Type.DEFINES, rel1.type());
        assertEquals(10, rel1.properties().get("line"));

        assertEquals(1, graph.statistics().relationshipCount());
        assertEquals(1, graph.relationships().size());

        // Try creating duplicate relationship
        GraphRelationship rel2 = relBuilder.createOrGetRelationship(
            "file-app",
            "sym-main",
            GraphRelationship.Type.DEFINES,
            Collections.singletonMap("line", 20)
        );

        assertEquals(rel1, rel2);
        assertEquals(1, graph.statistics().relationshipCount()); // count remains 1
        assertEquals(1, graph.relationships().size());
    }

    @Test
    void testRelationshipValidationMissingNodes() {
        KnowledgeGraph graph = new KnowledgeGraph("project-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);
        GraphValidator validator = new GraphValidator();

        nodeBuilder.createOrGetNode("n1", GraphNode.Type.FILE, "App.java", Collections.emptyMap());

        // Target node missing
        relBuilder.createOrGetRelationship("n1", "missing-target", GraphRelationship.Type.DEFINES, Collections.emptyMap());
        GraphValidationResult result1 = validator.validate(graph);
        assertFalse(result1.isValid());
        assertTrue(result1.errors().stream().anyMatch(e -> e.contains("missing target node ID")));

        // Source node missing
        KnowledgeGraph graph2 = new KnowledgeGraph("project-1", new KnowledgeGraphConfiguration());
        KnowledgeNodeBuilder nodeBuilder2 = new KnowledgeNodeBuilder(graph2);
        KnowledgeRelationshipBuilder relBuilder2 = new KnowledgeRelationshipBuilder(graph2);
        nodeBuilder2.createOrGetNode("n1", GraphNode.Type.FILE, "App.java", Collections.emptyMap());
        
        relBuilder2.createOrGetRelationship("missing-source", "n1", GraphRelationship.Type.DEFINES, Collections.emptyMap());
        GraphValidationResult result2 = validator.validate(graph2);
        assertFalse(result2.isValid());
        assertTrue(result2.errors().stream().anyMatch(e -> e.contains("missing source node ID")));
    }

    @Test
    void testSelfLoopsValidation() {
        GraphValidator validator = new GraphValidator();

        // By default self loops are disallowed
        KnowledgeGraph graph1 = new KnowledgeGraph("project-1", new KnowledgeGraphConfiguration(false, true));
        KnowledgeNodeBuilder nodeBuilder1 = new KnowledgeNodeBuilder(graph1);
        KnowledgeRelationshipBuilder relBuilder1 = new KnowledgeRelationshipBuilder(graph1);

        nodeBuilder1.createOrGetNode("n1", GraphNode.Type.FILE, "App.java", Collections.emptyMap());
        relBuilder1.createOrGetRelationship("n1", "n1", GraphRelationship.Type.REFERENCES, Collections.emptyMap());
        GraphValidationResult result = validator.validate(graph1);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Forbidden self loop")));

        // Allow self loops explicitly via configuration
        KnowledgeGraph graph2 = new KnowledgeGraph("project-1", new KnowledgeGraphConfiguration(true, true));
        KnowledgeNodeBuilder nodeBuilder2 = new KnowledgeNodeBuilder(graph2);
        KnowledgeRelationshipBuilder relBuilder2 = new KnowledgeRelationshipBuilder(graph2);

        nodeBuilder2.createOrGetNode("n1", GraphNode.Type.FILE, "App.java", Collections.emptyMap());
        GraphRelationship rel = relBuilder2.createOrGetRelationship("n1", "n1", GraphRelationship.Type.REFERENCES, Collections.emptyMap());
        assertNotNull(rel);
        assertEquals("n1", rel.sourceNodeId());
        assertEquals("n1", rel.targetNodeId());
        assertEquals(1, graph2.statistics().relationshipCount());
        
        GraphValidationResult result2 = validator.validate(graph2);
        assertFalse(result2.errors().stream().anyMatch(e -> e.contains("Forbidden self loop")));
    }
}
