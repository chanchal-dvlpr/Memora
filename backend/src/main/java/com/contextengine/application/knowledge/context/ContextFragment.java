package com.contextengine.application.knowledge.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable DTO representing a semantic slice of the project knowledge.
 */
public record ContextFragment(
    String fragmentId,
    ContextFragmentType fragmentType,
    String sourceNodeId,
    String sourcePath,
    String title,
    String content,
    Map<String, Object> metadata,
    int estimatedTokens
) {
    public ContextFragment {
        Objects.requireNonNull(fragmentId, "fragmentId must not be null");
        Objects.requireNonNull(fragmentType, "fragmentType must not be null");
        Objects.requireNonNull(sourceNodeId, "sourceNodeId must not be null");
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(content, "content must not be null");
        metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<>(metadata)) : Collections.emptyMap();
        if (estimatedTokens < 0) {
            throw new IllegalArgumentException("estimatedTokens must be non-negative");
        }
    }
}
