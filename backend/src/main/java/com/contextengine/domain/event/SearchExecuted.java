package com.contextengine.domain.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a lexical or vector search query is executed.
 */
public record SearchExecuted(
    String queryText,
    String searchType,
    long searchTimeMs,
    int resultsCount,
    Instant occurredAt
) implements DomainEvent {
    
    public SearchExecuted {
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        
        if (queryText == null || queryText.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text must not be null or empty");
        }
        if (searchType == null || searchType.trim().isEmpty()) {
            throw new IllegalArgumentException("Search type must not be null or empty");
        }
        if (searchTimeMs < 0) {
            throw new IllegalArgumentException("Search time must be non-negative");
        }
        if (resultsCount < 0) {
            throw new IllegalArgumentException("Results count must be non-negative");
        }
    }
}
