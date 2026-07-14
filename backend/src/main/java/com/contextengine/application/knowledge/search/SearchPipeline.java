package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import java.util.Objects;

/**
 * Pipeline coordinating input parameters to SearchEngine runs.
 */
public class SearchPipeline {
    private final SearchEngine engine;

    public SearchPipeline(SearchEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    public SearchResult execute(
        KnowledgeGraph graph,
        SearchQuery query,
        SearchConfiguration configuration
    ) {
        return execute(graph, query, configuration, "", false);
    }

    public SearchResult execute(
        KnowledgeGraph graph,
        SearchQuery query,
        SearchConfiguration configuration,
        String structuralHash,
        boolean isIncremental
    ) {
        SearchContext context = new SearchContext(graph, query, configuration, structuralHash, isIncremental);
        return engine.search(context);
    }
}
