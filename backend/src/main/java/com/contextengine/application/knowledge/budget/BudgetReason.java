package com.contextengine.application.knowledge.budget;

import java.util.Objects;

/**
 * Represents the extensible reason for a budget allocation decision.
 */
public record BudgetReason(String name, String description) {

    public static final BudgetReason NONE = new BudgetReason("NONE", "No budget constraint applied");
    public static final BudgetReason TOKEN_LIMIT = new BudgetReason("TOKEN_LIMIT", "Token limit exceeded");
    public static final BudgetReason CHARACTER_LIMIT = new BudgetReason("CHARACTER_LIMIT", "Character limit exceeded");
    public static final BudgetReason MAX_FRAGMENT_LIMIT = new BudgetReason("MAX_FRAGMENT_LIMIT", "Maximum fragment count limit reached");
    public static final BudgetReason LOW_RANK = new BudgetReason("LOW_RANK", "Rank score is below inclusion threshold");
    public static final BudgetReason RESERVED_SYSTEM_CONTEXT = new BudgetReason("RESERVED_SYSTEM_CONTEXT", "Reserved system context");

    public BudgetReason {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
