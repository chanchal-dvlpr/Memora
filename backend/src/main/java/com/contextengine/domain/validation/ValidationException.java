package com.contextengine.domain.validation;

/**
 * Thrown when domain validation constraints or rules are violated.
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
}
