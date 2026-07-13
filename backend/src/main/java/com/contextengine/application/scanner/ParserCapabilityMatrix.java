package com.contextengine.application.scanner;

import java.util.Map;
import java.util.Objects;

/**
 * Matrix representing parser features and capabilities.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 * <p>
 * Future Usage: Queried during parser routing to verify symbol support.
 * </p>
 */
public record ParserCapabilityMatrix(
    Map<String, Boolean> features
) {
    /**
     * Constructs a ParserCapabilityMatrix.
     *
     * @param features map of features and their support status
     */
    public ParserCapabilityMatrix {
        Objects.requireNonNull(features, "Features must not be null");
    }
}
