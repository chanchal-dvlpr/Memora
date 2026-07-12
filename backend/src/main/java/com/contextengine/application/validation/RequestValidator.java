package com.contextengine.application.validation;

/**
 * Interface contract for application-level query or command precondition checks.
 *
 * @param <T> the type of request to validate
 */
public interface RequestValidator<T> {
    
    /**
     * Validates the input request parameters.
     *
     * @param request the request containing values
     * @throws IllegalArgumentException if validation rules are violated
     */
    void validate(T request);
}
