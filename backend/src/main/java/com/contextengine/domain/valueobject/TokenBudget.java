package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;

/**
 * Models the maximum allowed token budget for compiled context configurations.
 */
public record TokenBudget(int value) implements ValueObject {
    
    /**
     * Constructs a TokenBudget and validates that the value is positive.
     *
     * @param value the token budget size
     * @throws IllegalArgumentException if value is less than or equal to 0
     */
    public TokenBudget {
        if (value <= 0) {
            throw new IllegalArgumentException("Token budget value must be positive (> 0)");
        }
    }
}
