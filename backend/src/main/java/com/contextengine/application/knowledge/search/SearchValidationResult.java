package com.contextengine.application.knowledge.search;

import java.util.List;
import java.util.Objects;

/**
 * Encapsulates search results validation outcomes.
 */
public record SearchValidationResult(boolean isValid, List<String> errors) {
    public SearchValidationResult {
        Objects.requireNonNull(errors, "errors list must not be null");
        errors = List.copyOf(errors);
    }
}
