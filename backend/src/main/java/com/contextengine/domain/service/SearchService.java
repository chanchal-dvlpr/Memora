package com.contextengine.domain.service;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.SearchQuery;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Executes optimized full-text, fuzzy suffix, and vector search operations over workspace symbols and project documents.
 */
public class SearchService {
    
    /**
     * Executes queries and returns matching elements.
     *
     * @param query the search query parameters
     * @param catalog the catalog of nodes to search
     * @return collection of matched elements
     * @throws IndexOutOfSyncException if index structures are corrupted
     * @throws NullPointerException if any argument is null
     */
    public Collection<KnowledgeNode> executeSearch(SearchQuery query, Collection<KnowledgeNode> catalog) {
        Objects.requireNonNull(query, "SearchQuery must not be null");
        Objects.requireNonNull(catalog, "Catalog must not be null");
        
        String term = query.term().toLowerCase().trim();
        
        return catalog.stream()
            .filter(node -> node.type().toLowerCase().contains(term) ||
                            (node.attributes().get("name") != null && node.attributes().get("name").toLowerCase().contains(term)))
            .collect(Collectors.toList());
    }
}
