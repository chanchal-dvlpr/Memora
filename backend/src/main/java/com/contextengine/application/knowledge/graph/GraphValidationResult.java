package com.contextengine.application.knowledge.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates structured graph validation details and errors list.
 */
public class GraphValidationResult {

    private final boolean isValid;
    private final List<String> errors;

    /**
     * Constructs a GraphValidationResult.
     *
     * @param errors list of identified graph violations
     */
    public GraphValidationResult(List<String> errors) {
        this.errors = errors != null ? new ArrayList<>(errors) : Collections.emptyList();
        this.isValid = this.errors.isEmpty();
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }
}
