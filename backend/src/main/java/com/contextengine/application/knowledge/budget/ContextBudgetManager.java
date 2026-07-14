package com.contextengine.application.knowledge.budget;

/**
 * Port contract for the Context Budget Manager.
 */
public interface ContextBudgetManager {

    /**
     * Constraints context fragments to fit within token, character, and max fragment thresholds.
     *
     * @param context the context to budget
     * @return the result carrying constrained fragments and statistics
     */
    BudgetResult budget(BudgetContext context);
}
