package com.contextengine.application.knowledge.budget;

/**
 * Extended statistics collected during context budgeting executions.
 */
public class BudgetStatistics {

    private int fragmentsEvaluated;
    private int fragmentsIncluded;
    private int fragmentsExcluded;
    private int partiallyIncluded;
    private int reservedFragments;
    private int estimatedTokensUsed;
    private int estimatedCharactersUsed;
    private long validationDuration;
    private long processingDuration;
    private int warnings;

    public int fragmentsEvaluated() {
        return fragmentsEvaluated;
    }

    public void incrementFragmentsEvaluated(int count) {
        this.fragmentsEvaluated += count;
    }

    public int fragmentsIncluded() {
        return fragmentsIncluded;
    }

    public void incrementFragmentsIncluded(int count) {
        this.fragmentsIncluded += count;
    }

    public int fragmentsExcluded() {
        return fragmentsExcluded;
    }

    public void incrementFragmentsExcluded(int count) {
        this.fragmentsExcluded += count;
    }

    public int partiallyIncluded() {
        return partiallyIncluded;
    }

    public void incrementPartiallyIncluded(int count) {
        this.partiallyIncluded += count;
    }

    public int reservedFragments() {
        return reservedFragments;
    }

    public void incrementReservedFragments(int count) {
        this.reservedFragments += count;
    }

    public int estimatedTokensUsed() {
        return estimatedTokensUsed;
    }

    public void incrementEstimatedTokensUsed(int count) {
        this.estimatedTokensUsed += count;
    }

    public int estimatedCharactersUsed() {
        return estimatedCharactersUsed;
    }

    public void incrementEstimatedCharactersUsed(int count) {
        this.estimatedCharactersUsed += count;
    }

    public long validationDuration() {
        return validationDuration;
    }

    public void setValidationDuration(long validationDuration) {
        this.validationDuration = validationDuration;
    }

    public long processingDuration() {
        return processingDuration;
    }

    public void setProcessingDuration(long processingDuration) {
        this.processingDuration = processingDuration;
    }

    public int warnings() {
        return warnings;
    }

    public void incrementWarnings(int count) {
        this.warnings += count;
    }
}
