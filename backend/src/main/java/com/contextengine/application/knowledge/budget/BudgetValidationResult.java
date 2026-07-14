package com.contextengine.application.knowledge.budget;

import java.util.List;
import java.util.Objects;

/**
 * Carrier holding outcome states of a budget validator check.
 */
public record BudgetValidationResult(boolean isValid, List<String> errors) {

    public BudgetValidationResult {
        Objects.requireNonNull(errors, "errors list must not be null");
        errors = List.copyOf(errors);
    }
}
