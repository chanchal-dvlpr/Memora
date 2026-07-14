package com.contextengine.application.knowledge.budget;

/**
 * Configuration limits for allocating context within allowed budgets.
 */
public class BudgetConfiguration {

    private final int maximumTokens;
    private final int maximumCharacters;
    private final int maximumFragments;
    private final int reserveSystemTokens;
    private final int reserveResponseTokens;

    /**
     * Default configurations.
     */
    public BudgetConfiguration() {
        this(100000, 400000, 100, 10000, 20000);
    }

    /**
     * Constructs configurations with custom limits.
     */
    public BudgetConfiguration(
        int maximumTokens,
        int maximumCharacters,
        int maximumFragments,
        int reserveSystemTokens,
        int reserveResponseTokens
    ) {
        this.maximumTokens = maximumTokens;
        this.maximumCharacters = maximumCharacters;
        this.maximumFragments = maximumFragments;
        this.reserveSystemTokens = reserveSystemTokens;
        this.reserveResponseTokens = reserveResponseTokens;
    }

    public int maximumTokens() {
        return maximumTokens;
    }

    public int maximumCharacters() {
        return maximumCharacters;
    }

    public int maximumFragments() {
        return maximumFragments;
    }

    public int reserveSystemTokens() {
        return reserveSystemTokens;
    }

    public int reserveResponseTokens() {
        return reserveResponseTokens;
    }
}
