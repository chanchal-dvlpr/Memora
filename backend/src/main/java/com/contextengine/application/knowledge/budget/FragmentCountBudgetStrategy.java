package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankedFragment;

/**
 * Strategy for limiting the total number of context fragments.
 */
public class FragmentCountBudgetStrategy implements BudgetAlgorithm {

    @Override
    public BudgetedFragment allocate(RankedFragment fragment, BudgetConfiguration config, BudgetState state) {
        if (state.accumulatedFragments() >= config.maximumFragments()) {
            return new BudgetedFragment(fragment, BudgetDecision.EXCLUDED, BudgetReason.MAX_FRAGMENT_LIMIT);
        }
        return null;
    }
}
