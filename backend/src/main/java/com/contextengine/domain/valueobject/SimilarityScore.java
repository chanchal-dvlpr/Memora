package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;

/**
 * Quantifies semantic relevance and vector match metrics.
 * Bounded within the interval [0.0, 1.0] and evaluates equality within a 10^-7 precision tolerance.
 */
public record SimilarityScore(double value) implements ValueObject, Comparable<SimilarityScore> {
    
    private static final double EPSILON = 1e-7;

    /**
     * Constructs a SimilarityScore and validates that the value is within [0.0, 1.0].
     *
     * @param value the similarity score value
     * @throws IllegalArgumentException if value is not bounded within [0.0, 1.0]
     */
    public SimilarityScore {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("SimilarityScore must be bounded within [0.0, 1.0]: " + value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SimilarityScore other)) return false;
        return Math.abs(this.value - other.value) < EPSILON;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(value / EPSILON));
    }

    @Override
    public int compareTo(SimilarityScore other) {
        return Double.compare(this.value, other.value);
    }
}
