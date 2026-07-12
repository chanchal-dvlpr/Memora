package com.contextengine.domain.validation;

/**
 * Standard contract for domain business validators.
 *
 * @param <T> the type of domain entity or aggregate root to validate
 */
public interface Validator<T> {
    
    /**
     * Validates the candidate against business invariants.
     *
     * @param candidate the object to validate
     * @throws ValidationException if a validation rule is violated
     */
    void validate(T candidate) throws ValidationException;
}
