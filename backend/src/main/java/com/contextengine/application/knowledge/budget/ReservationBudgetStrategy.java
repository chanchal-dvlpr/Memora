package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.context.ContextFragmentType;
import com.contextengine.application.knowledge.ranking.RankedFragment;

/**
 * Strategy for reservation of project and workspace system metadata.
 */
public class ReservationBudgetStrategy implements BudgetAlgorithm {

    @Override
    public BudgetedFragment allocate(RankedFragment fragment, BudgetConfiguration config, BudgetState state) {
        ContextFragmentType type = fragment.fragment().fragmentType();
        if (type == ContextFragmentType.PROJECT || type == ContextFragmentType.WORKSPACE) {
            int charLength = fragment.fragment().content() != null ? fragment.fragment().content().length() : 0;
            int estimatedTokens = Math.max(1, charLength / 4);

            state.incrementCharacters(charLength);
            state.incrementTokens(estimatedTokens);

            return new BudgetedFragment(fragment, BudgetDecision.RESERVED, BudgetReason.RESERVED_SYSTEM_CONTEXT);
        }
        return null;
    }
}
