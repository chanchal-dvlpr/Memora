package com.contextengine.application.knowledge.ranking;

import java.util.Collections;
import java.util.List;

/**
 * Structural carrier mapping validation success status and error messages list.
 */
public record RankingValidationResult(
    boolean isValid,
    List<String> errors
) {
    public RankingValidationResult {
        errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
    }
}
