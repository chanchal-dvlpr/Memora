package com.contextengine.application.knowledge.context;

import com.contextengine.application.knowledge.graph.GraphNode;
import com.contextengine.application.knowledge.graph.GraphRelationship;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates the structurally assembled ContextFragments against the source KnowledgeGraph.
 */
public class ContextValidator {

    /**
     * Validates the assembled context fragments list against the graph topology.
     *
     * @param fragments selected fragments list
     * @param graph     source knowledge graph
     * @return validation result holding errors, if any
     */
    public ContextValidationResult validate(List<ContextFragment> fragments, KnowledgeGraph graph) {
        Objects.requireNonNull(fragments, "Fragments list must not be null");
        Objects.requireNonNull(graph, "Graph must not be null");

        List<String> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Map<String, Integer> fragmentPositions = new HashMap<>();

        for (int i = 0; i < fragments.size(); i++) {
            ContextFragment fragment = fragments.get(i);
            fragmentPositions.put(fragment.sourceNodeId(), i);

            // 1. Duplicate fragment IDs
            if (!seenIds.add(fragment.fragmentId())) {
                errors.add("Duplicate fragment ID detected: " + fragment.fragmentId());
            }

            // 2. Invalid source node references
            GraphNode node = graph.getNode(fragment.sourceNodeId());
            if (node == null) {
                errors.add("Fragment references non-existent source node in graph: " + fragment.sourceNodeId());
                continue;
            }

            // 3. Invalid fragment type mappings
            if (!isValidTypeMapping(fragment.fragmentType(), node.type())) {
                errors.add("Invalid type mapping: fragment type " + fragment.fragmentType() + 
                           " does not match node type " + node.type() + " for node: " + node.id());
            }

            // 4. Missing required metadata
            if (node.type() == GraphNode.Type.FILE && !node.properties().containsKey("language")) {
                errors.add("Missing required metadata 'language' for file node: " + node.id());
            }
            if (node.type() == GraphNode.Type.SYMBOL && !node.properties().containsKey("kind")) {
                errors.add("Missing required metadata 'kind' for symbol node: " + node.id());
            }
        }

        // 5. Orphan fragments and invalid fragment ordering
        Set<String> containsTargets = new HashSet<>();
        for (GraphRelationship rel : graph.relationships()) {
            if (rel.type() == GraphRelationship.Type.CONTAINS || rel.type() == GraphRelationship.Type.DEFINES) {
                containsTargets.add(rel.targetNodeId());
                
                Integer sourcePos = fragmentPositions.get(rel.sourceNodeId());
                Integer targetPos = fragmentPositions.get(rel.targetNodeId());
                if (sourcePos != null && targetPos != null && sourcePos > targetPos) {
                    errors.add("Invalid fragment ordering: Parent fragment " + rel.sourceNodeId() + 
                               " appears after child fragment " + rel.targetNodeId());
                }
            }
        }

        for (ContextFragment fragment : fragments) {
            GraphNode node = graph.getNode(fragment.sourceNodeId());
            if (node != null && (node.type() == GraphNode.Type.DIRECTORY || node.type() == GraphNode.Type.FILE || node.type() == GraphNode.Type.SYMBOL)) {
                // If graph contains more than root PROJECT/WORKSPACE nodes, check containment
                if (!containsTargets.contains(node.id()) && graph.nodes().size() > 2) {
                    errors.add("Orphan fragment detected (no incoming containment/definition relation): " + fragment.sourceNodeId());
                }
            }
        }

        return new ContextValidationResult(errors);
    }

    private boolean isValidTypeMapping(ContextFragmentType fragmentType, GraphNode.Type nodeType) {
        return switch (fragmentType) {
            case PROJECT -> nodeType == GraphNode.Type.PROJECT;
            case WORKSPACE -> nodeType == GraphNode.Type.WORKSPACE;
            case MODULE -> nodeType == GraphNode.Type.MODULE;
            case DIRECTORY -> nodeType == GraphNode.Type.DIRECTORY;
            case FILE -> nodeType == GraphNode.Type.FILE;
            case SYMBOL -> nodeType == GraphNode.Type.SYMBOL;
            case DEPENDENCY -> nodeType == GraphNode.Type.DEPENDENCY;
        };
    }
}
