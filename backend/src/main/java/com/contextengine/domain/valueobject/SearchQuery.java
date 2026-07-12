package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;

/**
 * Encapsulates text search inputs, wildcards, filters, and target retrieval boundaries.
 */
public record SearchQuery(
    String term,
    boolean wildcardsEnabled,
    Metadata filters,
    int maxResults
) implements ValueObject {
    
    /**
     * Constructs a SearchQuery and validates that search terms are non-empty and filters are defined.
     *
     * @param term the search text term
     * @param wildcardsEnabled whether wildcard patterns are enabled
     * @param filters query filters represented via Metadata
     * @param maxResults maximum boundary count of results
     * @throws NullPointerException if term or filters is null
     * @throws IllegalArgumentException if term is empty/blank, or if maxResults is non-positive
     */
    public SearchQuery {
        Objects.requireNonNull(term, "Search term must not be null");
        Objects.requireNonNull(filters, "Filters must not be null");
        if (term.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term must be non-empty");
        }
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Max results must be greater than zero");
        }
    }
}
