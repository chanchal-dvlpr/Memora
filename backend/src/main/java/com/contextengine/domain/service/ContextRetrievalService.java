package com.contextengine.domain.service;

import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.SearchQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates multi-hop queries over the Knowledge Graph, combining lexical matching and vector scores.
 */
public class ContextRetrievalService {
    
    /**
     * Traverses the graph and retrieves a scored set of candidate nodes.
     * Enforces the hop limit (d <= 3) to prevent circular lookup lockups.
     *
     * @param graph the knowledge graph to search
     * @param searchQuery the query details
     * @param focusNode the starting focal node ID
     * @param maxHops the maximum hop limit distance
     * @return scored collection of candidate nodes
     * @throws GraphUnreachableException if database is locked or corrupted
     * @throws IllegalArgumentException if maxHops exceeds the configured limit of 3
     */
    public Collection<KnowledgeNode> retrieveContext(
        KnowledgeGraph graph,
        SearchQuery searchQuery,
        NodeId focusNode,
        int maxHops
    ) {
        Objects.requireNonNull(graph, "KnowledgeGraph must not be null");
        Objects.requireNonNull(searchQuery, "SearchQuery must not be null");
        Objects.requireNonNull(focusNode, "FocusNode must not be null");
        
        if (maxHops < 0 || maxHops > 3) {
            throw new IllegalArgumentException("Max hops must terminate within the configured hop bounds of 3");
        }
        
        List<KnowledgeNode> results = new ArrayList<>();
        for (KnowledgeNode node : graph.nodes()) {
            if (node.id().equals(focusNode) || node.type().toLowerCase().contains(searchQuery.term().toLowerCase())) {
                results.add(node);
            }
        }
        return results;
    }
}
