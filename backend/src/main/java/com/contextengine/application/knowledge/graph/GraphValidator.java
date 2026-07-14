package com.contextengine.application.knowledge.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates in-memory KnowledgeGraphs, performing cycle detection, self-loops,
 * endpoint, missing parent, and orphan node verification.
 */
public class GraphValidator {

    /**
     * Runs structural validation checks on the target graph.
     *
     * @param graph target KnowledgeGraph to validate
     * @return GraphValidationResult carrying validation success status and errors list
     */
    public GraphValidationResult validate(KnowledgeGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null");
        List<String> errors = new ArrayList<>();

        // 1. Validate relationship endpoints
        for (GraphRelationship rel : graph.relationships()) {
            if (graph.getNode(rel.sourceNodeId()) == null) {
                errors.add("Relationship references missing source node ID: " + rel.sourceNodeId());
            }
            if (graph.getNode(rel.targetNodeId()) == null) {
                errors.add("Relationship references missing target node ID: " + rel.targetNodeId());
            }

            // 2. Self loops validation
            if (rel.sourceNodeId().equals(rel.targetNodeId()) && !graph.configuration().allowSelfLoops()) {
                errors.add("Forbidden self loop detected on node ID: " + rel.sourceNodeId());
            }
        }

        // 3. Cycle detection for CONTAINS relationships
        checkContainsCycle(graph, errors);

        // 4. Missing parent relationships check (e.g. nested files/directories must be CONTAINS targets)
        Set<String> containsTargets = new HashSet<>();
        for (GraphRelationship rel : graph.relationships()) {
            if (rel.type() == GraphRelationship.Type.CONTAINS) {
                containsTargets.add(rel.targetNodeId());
            }
        }
        for (GraphNode node : graph.nodes()) {
            // Directories and files must have parent CONTAINS relationships.
            if (node.type() == GraphNode.Type.DIRECTORY || node.type() == GraphNode.Type.FILE) {
                if (!containsTargets.contains(node.id())) {
                    errors.add("Node lacks a parent CONTAINS relationship: " + node.id() + " (" + node.type() + ")");
                }
            }
        }

        // 5. Orphan nodes detection
        Set<String> connectedNodes = new HashSet<>();
        for (GraphRelationship rel : graph.relationships()) {
            connectedNodes.add(rel.sourceNodeId());
            connectedNodes.add(rel.targetNodeId());
        }
        for (GraphNode node : graph.nodes()) {
            // A node is an orphan if it has no incoming/outgoing relationships, unless it's a standalone Project node in an empty workspace.
            if (graph.nodes().size() > 1 && !connectedNodes.contains(node.id())) {
                errors.add("Orphan node detected: " + node.id() + " (" + node.type() + ")");
            }
        }

        return new GraphValidationResult(errors);
    }

    private void checkContainsCycle(KnowledgeGraph graph, List<String> errors) {
        Map<String, List<String>> adj = new HashMap<>();
        for (GraphRelationship rel : graph.relationships()) {
            if (rel.type() == GraphRelationship.Type.CONTAINS) {
                adj.computeIfAbsent(rel.sourceNodeId(), k -> new ArrayList<>()).add(rel.targetNodeId());
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (GraphNode node : graph.nodes()) {
            if (hasCycleDfs(node.id(), adj, visited, recStack)) {
                errors.add("Circular CONTAINS relationship detected involving node: " + node.id());
                return; // Report first cycle found
            }
        }
    }

    private boolean hasCycleDfs(String u, Map<String, List<String>> adj, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(u)) {
            return true;
        }
        if (visited.contains(u)) {
            return false;
        }

        visited.add(u);
        recStack.add(u);

        List<String> neighbors = adj.get(u);
        if (neighbors != null) {
            for (String v : neighbors) {
                if (hasCycleDfs(v, adj, visited, recStack)) {
                    return true;
                }
            }
        }

        recStack.remove(u);
        return false;
    }
}
