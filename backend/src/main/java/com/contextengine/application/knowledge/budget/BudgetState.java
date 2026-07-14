package com.contextengine.application.knowledge.budget;

/**
 * Mutable state tracker for context budgeting evaluations.
 */
public class BudgetState {

    private int accumulatedFragments;
    private int accumulatedCharacters;
    private int accumulatedTokens;

    public int accumulatedFragments() {
        return accumulatedFragments;
    }

    public void incrementFragments(int count) {
        this.accumulatedFragments += count;
    }

    public int accumulatedCharacters() {
        return accumulatedCharacters;
    }

    public void incrementCharacters(int count) {
        this.accumulatedCharacters += count;
    }

    public int accumulatedTokens() {
        return accumulatedTokens;
    }

    public void incrementTokens(int count) {
        this.accumulatedTokens += count;
    }
}
