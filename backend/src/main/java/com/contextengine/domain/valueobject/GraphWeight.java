package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;

/**
 * Represents decay levels and relational strength parameters of edges in the Knowledge Graph.
 * Must be a positive real number (> 0.0).
 */
public record GraphWeight(double value) implements ValueObject, Comparable<GraphWeight> {
    
    /**
     * Constructs a GraphWeight and validates that the value is a positive real number (> 0.0).
     *
     * @param value the weight value
     * @throws IllegalArgumentException if value is less than or equal to 0.0
     */
    public GraphWeight {
        if (value <= 0.0) {
            throw new IllegalArgumentException("GraphWeight must be a positive real number (> 0.0): " + value);
        }
    }

    @Override
    public int compareTo(GraphWeight other) {
        return Double.compare(this.value, other.value);
    }
}
