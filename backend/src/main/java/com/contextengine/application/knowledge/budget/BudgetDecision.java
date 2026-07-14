package com.contextengine.application.knowledge.budget;

/**
 * Represents the allocation decision for a context fragment.
 */
public enum BudgetDecision {
    /**
     * Fragment is fully included in the context.
     */
    INCLUDED,

    /**
     * Fragment is fully excluded from the context.
     */
    EXCLUDED,

    /**
     * Fragment is partially included in the context.
     */
    PARTIALLY_INCLUDED,

    /**
     * Fragment is reserved for critical system allocations.
     */
    RESERVED
}
