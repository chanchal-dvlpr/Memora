package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankedFragment;
import java.util.Objects;

/**
 * A wrapper representing a RankedFragment with its context budgeting allocation decision.
 */
public record BudgetedFragment(
    RankedFragment rankedFragment,
    BudgetDecision decision,
    BudgetReason reason
) {
    public BudgetedFragment {
        Objects.requireNonNull(rankedFragment, "rankedFragment must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
