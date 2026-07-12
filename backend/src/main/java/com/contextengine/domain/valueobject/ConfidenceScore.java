package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;

/**
 * Quantifies AI reasoning confidence levels.
 * Bounded within the interval [0.0, 1.0] and evaluates equality within a 10^-7 precision tolerance.
 */
public record ConfidenceScore(double value) implements ValueObject, Comparable<ConfidenceScore> {
    
    private static final double EPSILON = 1e-7;

    /**
     * Constructs a ConfidenceScore and validates that the value is within [0.0, 1.0].
     *
     * @param value the confidence score value
     * @throws IllegalArgumentException if value is not bounded within [0.0, 1.0]
     */
    public ConfidenceScore {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("ConfidenceScore must be bounded within [0.0, 1.0]: " + value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConfidenceScore other)) return false;
        return Math.abs(this.value - other.value) < EPSILON;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(value / EPSILON));
    }

    @Override
    public int compareTo(ConfidenceScore other) {
        return Double.compare(this.value, other.value);
    }
}
