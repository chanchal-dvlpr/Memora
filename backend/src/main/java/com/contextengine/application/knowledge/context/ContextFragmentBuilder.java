package com.contextengine.application.knowledge.context;

import com.contextengine.application.knowledge.graph.GraphNode;
import com.contextengine.application.knowledge.graph.GraphRelationship;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;

import java.util.*;

/**
 * Service converting KnowledgeGraph nodes into ContextFragments following deterministic traversal ordering.
 */
public class ContextFragmentBuilder {

    /**
     * Traverses the graph from the roots to assemble fragments.
     *
     * @param graph  target knowledge graph
     * @param config configuration parameters
     * @param stats  statistics to update
     * @return ordered list of unique context fragments
     */
    public List<ContextFragment> buildFragments(KnowledgeGraph graph, ContextAssemblyConfiguration config, ContextStatistics stats) {
        return buildFragments(graph, config, stats, Collections.emptyMap());
    }

    /**
     * Traverses the graph from the roots to assemble fragments, utilizing reused fragments for incremental optimization.
     */
    public List<ContextFragment> buildFragments(
        KnowledgeGraph graph,
        ContextAssemblyConfiguration config,
        ContextStatistics stats,
        Map<String, ContextFragment> reusedFragments
    ) {
        Objects.requireNonNull(graph, "Graph must not be null");
        Objects.requireNonNull(config, "Config must not be null");
        Objects.requireNonNull(stats, "Stats must not be null");
        Objects.requireNonNull(reusedFragments, "ReusedFragments must not be null");

        List<ContextFragment> fragments = new ArrayList<>();
        Set<String> visitedIds = new HashSet<>();
        Set<String> addedFragmentIds = new HashSet<>();

        // 1. Identify Project nodes as main roots and sort them lexicographically to ensure determinism
        List<GraphNode> projectNodes = new ArrayList<>();
        for (GraphNode node : graph.nodes()) {
            if (node.type() == GraphNode.Type.PROJECT) {
                projectNodes.add(node);
            }
        }
        projectNodes.sort(Comparator.comparing(GraphNode::id));

        // 2. Perform traversal starting from Project roots
        for (GraphNode root : projectNodes) {
            traverse(root.id(), 0, fragments, visitedIds, addedFragmentIds, graph, config, stats, reusedFragments);
        }

        // 3. Fallback: process any unvisited nodes to ensure complete coverage (e.g. module or dependency orphans)
        List<GraphNode> remainingNodes = new ArrayList<>(graph.nodes());
        remainingNodes.sort(Comparator.comparing(GraphNode::id));
        for (GraphNode node : remainingNodes) {
            if (!visitedIds.contains(node.id())) {
                traverse(node.id(), 0, fragments, visitedIds, addedFragmentIds, graph, config, stats, reusedFragments);
            }
        }

        return fragments;
    }

    private void traverse(
        String nodeId,
        int depth,
        List<ContextFragment> fragments,
        Set<String> visitedIds,
        Set<String> addedFragmentIds,
        KnowledgeGraph graph,
        ContextAssemblyConfiguration config,
        ContextStatistics stats,
        Map<String, ContextFragment> reusedFragments
    ) {
        if (depth > config.maximumAssemblyDepth()) {
            stats.incrementSkippedNodes(1);
            return;
        }

        boolean isFirstVisit = visitedIds.add(nodeId);
        GraphNode node = graph.getNode(nodeId);
        if (node == null) {
            return;
        }

        if (isFirstVisit) {
            String fragmentId = "fragment:" + node.id();
            if (addedFragmentIds.contains(fragmentId)) {
                stats.incrementDuplicateFragmentsRemoved(1);
            } else {
                ContextFragment fragment = convertToFragment(node, reusedFragments);
                fragments.add(fragment);
                addedFragmentIds.add(fragmentId);
                stats.incrementFragmentsCreated(1);
            }
        } else {
            // Node already traversed in this run, prevent cycles/duplicates
            return;
        }

        // Gather child relationships and sort lexicographically by target node ID
        List<GraphRelationship> outgoing = new ArrayList<>();
        for (GraphRelationship rel : graph.relationships()) {
            if (rel.sourceNodeId().equals(nodeId)) {
                outgoing.add(rel);
            }
        }
        outgoing.sort(Comparator.comparing(GraphRelationship::targetNodeId));

        for (GraphRelationship rel : outgoing) {
            if (rel.type() == GraphRelationship.Type.CONTAINS ||
                rel.type() == GraphRelationship.Type.DEFINES ||
                rel.type() == GraphRelationship.Type.DEPENDS_ON) {
                traverse(rel.targetNodeId(), depth + 1, fragments, visitedIds, addedFragmentIds, graph, config, stats, reusedFragments);
            }
        }
    }

    private ContextFragment convertToFragment(GraphNode node, Map<String, ContextFragment> reusedFragments) {
        if (reusedFragments.containsKey(node.id())) {
            return reusedFragments.get(node.id());
        }

        String fragmentId = "fragment:" + node.id();
        ContextFragmentType fragmentType = mapType(node.type());
        String sourceNodeId = node.id();

        String sourcePath = "";
        if (node.type() == GraphNode.Type.FILE) {
            sourcePath = node.id().replace("file:", "");
        } else if (node.type() == GraphNode.Type.DIRECTORY) {
            sourcePath = (String) node.properties().getOrDefault("relativePath", "");
        } else if (node.type() == GraphNode.Type.SYMBOL) {
            sourcePath = (String) node.properties().getOrDefault("filePath", "");
        }

        String title = node.label();
        String content = generateContent(node);
        Map<String, Object> metadata = node.properties();
        int estimatedTokens = (content.length() + 3) / 4;

        return new ContextFragment(
            fragmentId,
            fragmentType,
            sourceNodeId,
            sourcePath,
            title,
            content,
            metadata,
            estimatedTokens
        );
    }

    private ContextFragmentType mapType(GraphNode.Type type) {
        return switch (type) {
            case PROJECT -> ContextFragmentType.PROJECT;
            case WORKSPACE -> ContextFragmentType.WORKSPACE;
            case MODULE -> ContextFragmentType.MODULE;
            case DIRECTORY -> ContextFragmentType.DIRECTORY;
            case FILE -> ContextFragmentType.FILE;
            case SYMBOL -> ContextFragmentType.SYMBOL;
            case DEPENDENCY -> ContextFragmentType.DEPENDENCY;
        };
    }

    private String generateContent(GraphNode node) {
        return switch (node.type()) {
            case PROJECT -> "Project: " + node.label();
            case WORKSPACE -> "Workspace: " + node.label();
            case MODULE -> "Module: " + node.label();
            case DIRECTORY -> "Directory: " + node.label() + " (path: " + node.properties().getOrDefault("relativePath", "") + ")";
            case FILE -> "File: " + node.label() + " (language: " + node.properties().getOrDefault("language", "UNKNOWN") + ")";
            case SYMBOL -> "Symbol: " + node.label() + " (kind: " + node.properties().getOrDefault("kind", "") + ", lines: " + node.properties().getOrDefault("startLine", "") + "-" + node.properties().getOrDefault("endLine", "") + ")";
            case DEPENDENCY -> "Dependency: " + node.label() + " (version: " + node.properties().getOrDefault("version", "") + ", scope: " + node.properties().getOrDefault("scope", "") + ")";
        };
    }
}
