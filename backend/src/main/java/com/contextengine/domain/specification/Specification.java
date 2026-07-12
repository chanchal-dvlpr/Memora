package com.contextengine.domain.specification;

/**
 * Encapsulates reusable business rules as predicates in accordance with the Specification Pattern.
 *
 * @param <T> the candidate object type to evaluate
 */
public interface Specification<T> {
    
    /**
     * Evaluates the business rule against the provided candidate.
     *
     * @param candidate the object to evaluate
     * @return true if the candidate satisfies the specification, false otherwise
     */
    boolean isSatisfiedBy(T candidate);
}
