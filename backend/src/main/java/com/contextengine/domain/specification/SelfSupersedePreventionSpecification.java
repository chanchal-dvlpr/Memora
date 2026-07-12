package com.contextengine.domain.specification;

import com.contextengine.domain.entity.Decision;

/**
 * Enforces the DI-6 (Self-Supersede Prevention) rule: An Architecture Decision Record (ADR)
 * cannot supersede itself.
 */
public class SelfSupersedePreventionSpecification implements Specification<Decision> {

    @Override
    public boolean isSatisfiedBy(Decision decision) {
        if (decision == null) {
            return false;
        }
        return decision.supersededBy() == null || !decision.supersededBy().equals(decision.id());
    }
}
