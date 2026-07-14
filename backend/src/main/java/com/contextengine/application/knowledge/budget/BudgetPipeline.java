package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankingResult;
import java.util.Objects;

/**
 * Pipeline coordinating ranking result and budget constraints.
 */
public class BudgetPipeline {

    private final ContextBudgetManager budgetManager;

    public BudgetPipeline(ContextBudgetManager budgetManager) {
        this.budgetManager = Objects.requireNonNull(budgetManager, "budgetManager must not be null");
    }

    /**
     * Executes the budgeting pipeline.
     *
     * @param rankingResult the ranking result input
     * @param configuration the configuration boundaries
     * @return the constrained BudgetResult
     */
    public BudgetResult execute(RankingResult rankingResult, BudgetConfiguration configuration) {
        BudgetContext context = new BudgetContext(rankingResult, configuration);
        return budgetManager.budget(context);
    }
}
