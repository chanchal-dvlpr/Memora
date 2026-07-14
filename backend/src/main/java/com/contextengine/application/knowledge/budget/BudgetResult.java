package com.contextengine.application.knowledge.budget;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Result envelope returned by ContextBudgetManager carrying budgeted fragments and performance stats.
 */
public class BudgetResult {

    private final String projectId;
    private final List<BudgetedFragment> budgetedFragments;
    private final BudgetStatistics statistics;
    private final Instant timestamp;

    public BudgetResult(
        String projectId,
        List<BudgetedFragment> budgetedFragments,
        BudgetStatistics statistics,
        Instant timestamp
    ) {
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.budgetedFragments = List.copyOf(budgetedFragments);
        this.statistics = Objects.requireNonNull(statistics, "statistics must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public String projectId() {
        return projectId;
    }

    public List<BudgetedFragment> budgetedFragments() {
        return budgetedFragments;
    }

    public BudgetStatistics statistics() {
        return statistics;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
