package com.contextengine.application.knowledge.search;

import java.util.List;
import java.util.Objects;

/**
 * Result collection of a Search Engine query run.
 */
public record SearchResult(
    List<SearchHit> hits,
    SearchSummary summary,
    SearchStatistics statistics
) {
    public SearchResult {
        Objects.requireNonNull(hits, "hits must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(statistics, "statistics must not be null");
        hits = List.copyOf(hits);
    }
}
