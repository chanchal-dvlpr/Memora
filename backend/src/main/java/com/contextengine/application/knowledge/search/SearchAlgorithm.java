package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.GraphNode;
import java.util.List;

/**
 * Strategy interface for matching individual graph nodes against a query term.
 */
public interface SearchAlgorithm {

    /**
     * Evaluates a graph node against the search configuration.
     * If matched, constructs and appends a SearchHit to the hits collection.
     *
     * @param node   target graph node
     * @param term   search query term
     * @param config search configurations
     * @param hits   collection of hits to populate
     * @return true if match was found and appended, false otherwise
     */
    boolean match(
        GraphNode node,
        String term,
        SearchConfiguration config,
        List<SearchHit> hits
    );

    default SearchHit createHit(GraphNode node, SearchMatchType matchType, SearchConfiguration config) {
        java.util.Map<String, Object> metadata = config.includeMetadata() ? node.properties() : java.util.Collections.emptyMap();
        String entityPath = (String) node.properties().get("filePath");
        if (entityPath == null && node.type() == GraphNode.Type.FILE) {
            entityPath = node.id().replace("file:", "");
        }
        return new SearchHit(
            node.id(),
            node.type().name(),
            node.label(),
            entityPath != null ? entityPath : "",
            matchType,
            metadata
        );
    }
}
