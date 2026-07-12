package com.contextengine.domain.service;

/**
 * Thrown when target files exceed context token budget limits.
 */
public class BudgetUnderflowException extends RuntimeException {
    public BudgetUnderflowException(String message) {
        super(message);
    }
}
