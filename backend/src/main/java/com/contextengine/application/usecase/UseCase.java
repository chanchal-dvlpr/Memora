package com.contextengine.application.usecase;

/**
 * Represents a business use case boundary in Clean Architecture.
 * Coordinates domain aggregates, services, and ports to complete a single user goal.
 *
 * @param <I> the input DTO or value object type
 * @param <O> the output DTO or result type
 */
public interface UseCase<I, O> {
    
    /**
     * Executes the business use case.
     *
     * @param input the input parameters
     * @return the execution response outcome
     */
    O execute(I input);
}
