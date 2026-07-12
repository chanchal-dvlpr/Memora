package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.List;
import java.util.Objects;

/**
 * Measures the alignment and grounding confidence of compiled context configurations.
 * Bounded score from 0.0 to 1.0 alongside detailed assessment logs.
 */
public record AIConfidence(
    double score,
    List<String> assessmentLogs
) implements ValueObject {
    
    /**
     * Constructs an AIConfidence and validates that the score is bounded within [0.0, 1.0].
     *
     * @param score the alignment and grounding score
     * @param assessmentLogs the detailed list of validation logs
     * @throws IllegalArgumentException if score is not in the [0.0, 1.0] interval
     * @throws NullPointerException if assessmentLogs is null
     */
    public AIConfidence {
        Objects.requireNonNull(assessmentLogs, "Assessment logs must not be null");
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("AIConfidence score must be bounded within [0.0, 1.0]");
        }
        assessmentLogs = List.copyOf(assessmentLogs);
    }
}
