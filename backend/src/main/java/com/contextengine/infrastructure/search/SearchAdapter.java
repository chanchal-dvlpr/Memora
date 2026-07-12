package com.contextengine.infrastructure.search;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.SearchQuery;
import java.util.Collection;
import java.util.Objects;

/**
 * High-performance search adapter coordinating inverted index execution and result ranking.
 * <p>
 * Bounded Context: Search Engine
 * Related Subsystem: Search Engine Subsystem (SE-SUB)
 * </p>
 */
public class SearchAdapter {

    private final InvertedIndexManager indexManager;
    private final SearchCombiner searchCombiner;

    /**
     * Constructs a SearchAdapter.
     */
    public SearchAdapter() {
        this.indexManager = new InvertedIndexManager();
        this.searchCombiner = new SearchCombiner();
    }

    /**
     * Executes keyword search over the catalog using the inverted index and combiner.
     *
     * @param query the query parameters
     * @param catalog the catalog of nodes to search
     * @return matched and ranked nodes
     */
    public Collection<KnowledgeNode> executeSearch(SearchQuery query, Collection<KnowledgeNode> catalog) {
        Objects.requireNonNull(query, "Query must not be null");
        Objects.requireNonNull(catalog, "Catalog must not be null");
        
        indexManager.indexNodes(catalog);
        Collection<KnowledgeNode> matches = indexManager.findMatches(query.term());
        return searchCombiner.combineAndRank(matches, query.term());
    }
}
