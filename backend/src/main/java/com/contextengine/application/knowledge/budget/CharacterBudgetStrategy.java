package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankedFragment;

/**
 * Strategy for limiting the total number of characters in the context.
 */
public class CharacterBudgetStrategy implements BudgetAlgorithm {

    @Override
    public BudgetedFragment allocate(RankedFragment fragment, BudgetConfiguration config, BudgetState state) {
        int charLength = fragment.fragment().content() != null ? fragment.fragment().content().length() : 0;
        if (state.accumulatedCharacters() + charLength > config.maximumCharacters()) {
            return new BudgetedFragment(fragment, BudgetDecision.EXCLUDED, BudgetReason.CHARACTER_LIMIT);
        }
        return null;
    }
}
