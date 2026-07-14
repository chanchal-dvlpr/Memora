package com.contextengine.application.knowledge.search;

/**
 * Port contract for traversing project knowledge graph elements and evaluating queries.
 */
public interface SearchEngine {
    
    /**
     * Executes in-memory traversal and queries target nodes.
     *
     * @param context search configuration and graph context
     * @return search result matches and execution summary
     */
    SearchResult search(SearchContext context);
}
