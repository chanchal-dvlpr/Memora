package com.contextengine.application.knowledge.ranking;

/**
 * Port interface defining ranking operations.
 */
public interface RelevanceRankingEngine {
    RankingResult rank(RankingContext context);
}
