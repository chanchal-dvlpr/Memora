package com.contextengine.application.knowledge.context;

import java.util.Collections;
import java.util.List;

/**
 * Result envelope of the assembled context validation.
 */
public class ContextValidationResult {
    private final boolean isValid;
    private final List<String> errors;

    public ContextValidationResult(List<String> errors) {
        this.errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
        this.isValid = this.errors.isEmpty();
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> errors() {
        return errors;
    }
}
