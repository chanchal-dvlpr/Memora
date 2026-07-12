package com.contextengine.domain.specification;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.TokenBudget;
import java.util.Objects;

/**
 * Enforces the DI-3 (Token Budget Boundary) rule: Compiled context snapshot footprints
 * must not exceed the target configured token budget limits.
 */
public class TokenBudgetSpecification implements Specification<ContextSnapshot> {
    
    private final TokenBudget budget;

    /**
     * Constructs a TokenBudgetSpecification.
     *
     * @param budget the target token budget
     */
    public TokenBudgetSpecification(TokenBudget budget) {
        this.budget = Objects.requireNonNull(budget, "TokenBudget must not be null");
    }

    @Override
    public boolean isSatisfiedBy(ContextSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        return snapshot.summary().tokenFootprint() <= budget.value();
    }
}
