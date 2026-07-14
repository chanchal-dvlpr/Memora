package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankedFragment;

/**
 * Strategy for limiting token usage and finalizing the INCLUDED status of fragments within limits.
 */
public class TokenBudgetStrategy implements BudgetAlgorithm {

    @Override
    public BudgetedFragment allocate(RankedFragment fragment, BudgetConfiguration config, BudgetState state) {
        int charLength = fragment.fragment().content() != null ? fragment.fragment().content().length() : 0;
        int estimatedTokens = Math.max(1, charLength / 4);

        int availableTokensLimit = config.maximumTokens() - config.reserveSystemTokens() - config.reserveResponseTokens();

        if (state.accumulatedTokens() + estimatedTokens > availableTokensLimit) {
            return new BudgetedFragment(fragment, BudgetDecision.EXCLUDED, BudgetReason.TOKEN_LIMIT);
        }

        if (fragment.score().value() < 0.0) {
            return new BudgetedFragment(fragment, BudgetDecision.EXCLUDED, BudgetReason.LOW_RANK);
        }

        // Within all limits, mark as included and update cumulative state
        state.incrementFragments(1);
        state.incrementCharacters(charLength);
        state.incrementTokens(estimatedTokens);

        return new BudgetedFragment(fragment, BudgetDecision.INCLUDED, BudgetReason.NONE);
    }
}
