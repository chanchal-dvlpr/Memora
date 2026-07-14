package com.contextengine.application.knowledge.search;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates the user's search query terms and specific filters.
 */
public record SearchQuery(String term, Map<String, Object> options) {
    public SearchQuery {
        Objects.requireNonNull(term, "term must not be null");
        options = options != null ? Map.copyOf(options) : Collections.emptyMap();
    }
}
