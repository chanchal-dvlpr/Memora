package com.contextengine.domain.service;

import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeRelationship;
import java.util.Objects;

/**
 * Validates, structures, and updates the multi-dimensional semantic graph schema.
 */
public class KnowledgeGraphService {
    
    /**
     * Validates referential integrity for the knowledge graph.
     * Enforces the Zero Orphan Edges (DI-5) rule.
     *
     * @param graph the knowledge graph to validate
     * @return true if integrity is intact, false otherwise
     * @throws GraphIntegrityViolationException if an orphan relationship (missing source or target node) is detected
     * @throws NullPointerException if graph is null
     */
    public boolean validateIntegrity(KnowledgeGraph graph) {
        Objects.requireNonNull(graph, "KnowledgeGraph must not be null");
        
        for (KnowledgeRelationship rel : graph.relationships()) {
            boolean sourceExists = graph.nodes().stream().anyMatch(n -> n.id().equals(rel.sourceNodeId()));
            boolean targetExists = graph.nodes().stream().anyMatch(n -> n.id().equals(rel.targetNodeId()));
            if (!sourceExists || !targetExists) {
                throw new GraphIntegrityViolationException("Graph integrity check failed: orphan relationship detected for relationship ID " + rel.id().value());
            }
        }
        return true;
    }
}
