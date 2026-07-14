package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.graph.*;
import java.util.*;

/**
 * Reusable test factory generating test KnowledgeGraph structures.
 */
public class TestKnowledgeGraphFactory {

    public static KnowledgeGraph createEmptyGraph(String projectId) {
        return new KnowledgeGraph(projectId, new KnowledgeGraphConfiguration(false, true));
    }

    public static KnowledgeGraph createSingleFileGraph(String projectId) {
        KnowledgeGraph graph = createEmptyGraph(projectId);
        Map<String, Object> fileProps = new HashMap<>();
        fileProps.put("absolutePath", "/workspace/src/Main.java");
        fileProps.put("language", "JAVA");
        GraphNode node = new GraphNode("file:src/Main.java", GraphNode.Type.FILE, "Main.java", fileProps);
        graph.addNode(node);
        return graph;
    }

    public static KnowledgeGraph createGraphWithSymbolsAndDependencies(String projectId) {
        KnowledgeGraph graph = createSingleFileGraph(projectId);

        // Add a Symbol Node
        Map<String, Object> symProps = new HashMap<>();
        symProps.put("filePath", "src/Main.java");
        symProps.put("kind", "CLASS");
        GraphNode symNode = new GraphNode("symbol:src/Main.java:Main", GraphNode.Type.SYMBOL, "Main", symProps);
        graph.addNode(symNode);

        // Add Dependency Node
        Map<String, Object> depProps = new HashMap<>();
        depProps.put("version", "1.0.0");
        GraphNode depNode = new GraphNode("dep:spring-core", GraphNode.Type.DEPENDENCY, "spring-core", depProps);
        graph.addNode(depNode);

        // Add relationships
        graph.addRelationship(new GraphRelationship("file:src/Main.java", "symbol:src/Main.java:Main", GraphRelationship.Type.CONTAINS, Collections.emptyMap()));
        graph.addRelationship(new GraphRelationship("file:src/Main.java", "dep:spring-core", GraphRelationship.Type.DEPENDS_ON, Collections.emptyMap()));

        return graph;
    }
}
