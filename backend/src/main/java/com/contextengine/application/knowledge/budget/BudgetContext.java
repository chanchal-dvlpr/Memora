package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankingResult;
import java.util.Objects;

/**
 * Envelope holding parameters for context budgeting calculations.
 */
public class BudgetContext {

    private final RankingResult rankingResult;
    private final BudgetConfiguration configuration;
    private final String structuralHash;
    private final boolean isIncremental;
    private final java.util.List<String> dirtyPaths;

    public BudgetContext(RankingResult rankingResult, BudgetConfiguration configuration) {
        this(rankingResult, configuration, "", false, java.util.Collections.emptyList());
    }

    public BudgetContext(
        RankingResult rankingResult,
        BudgetConfiguration configuration,
        String structuralHash,
        boolean isIncremental,
        java.util.List<String> dirtyPaths
    ) {
        this.rankingResult = Objects.requireNonNull(rankingResult, "rankingResult must not be null");
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.structuralHash = structuralHash != null ? structuralHash : "";
        this.isIncremental = isIncremental;
        this.dirtyPaths = dirtyPaths != null ? java.util.List.copyOf(dirtyPaths) : java.util.Collections.emptyList();
    }

    public RankingResult rankingResult() {
        return rankingResult;
    }

    public BudgetConfiguration configuration() {
        return configuration;
    }

    public String structuralHash() {
        return structuralHash;
    }

    public boolean isIncremental() {
        return isIncremental;
    }

    public java.util.List<String> dirtyPaths() {
        return dirtyPaths;
    }
}
