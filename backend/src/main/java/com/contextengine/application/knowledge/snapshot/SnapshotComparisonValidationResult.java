package com.contextengine.application.knowledge.snapshot;

import java.util.List;
import java.util.Objects;

/**
 * Record holding outcomes of comparison validator checks.
 */
public record SnapshotComparisonValidationResult(boolean isValid, List<String> errors) {

    public SnapshotComparisonValidationResult {
        Objects.requireNonNull(errors, "errors list must not be null");
        errors = List.copyOf(errors);
    }
}
