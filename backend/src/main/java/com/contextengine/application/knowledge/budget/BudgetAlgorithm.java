package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankedFragment;

/**
 * Interface contract for budget evaluation strategies.
 */
public interface BudgetAlgorithm {

    /**
     * Allocates budget decision and reason for a ranked fragment.
     *
     * @param fragment the ranked fragment
     * @param config   budget limits
     * @param state    current cumulative allocation state
     * @return the budgeted fragment outcome, or null if this strategy does not handle the fragment
     */
    BudgetedFragment allocate(RankedFragment fragment, BudgetConfiguration config, BudgetState state);
}
