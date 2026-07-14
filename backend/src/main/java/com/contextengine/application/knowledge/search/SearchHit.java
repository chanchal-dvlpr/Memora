package com.contextengine.application.knowledge.search;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Single matched item in a search query result.
 */
public record SearchHit(
    String entityId,
    String entityType,
    String name,
    String path,
    SearchMatchType matchType,
    Map<String, Object> metadata
) {
    public SearchHit {
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(matchType, "matchType must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }
}
