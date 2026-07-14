package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import java.util.Objects;

/**
 * Encapsulates graph source target and search options.
 */
public record SearchContext(
    KnowledgeGraph graph,
    SearchQuery query,
    SearchConfiguration configuration,
    String structuralHash,
    boolean isIncremental
) {
    public SearchContext(
        KnowledgeGraph graph,
        SearchQuery query,
        SearchConfiguration configuration
    ) {
        this(graph, query, configuration, "", false);
    }

    public SearchContext {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        structuralHash = structuralHash != null ? structuralHash : "";
    }
}
