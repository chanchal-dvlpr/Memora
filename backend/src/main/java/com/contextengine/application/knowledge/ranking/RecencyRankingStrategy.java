package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;

/**
 * Strategy evaluating recency relevance weights.
 */
public class RecencyRankingStrategy implements RankingStrategy {

    @Override
    public RankingReason evaluate(ContextFragment fragment, RankingConfiguration config) {
        if (!config.enableRecencyWeight()) {
            return new RankingReason(RankingFactor.RECENCY, 0.0, "Recency weight disabled");
        }

        double score = 10.0; // Baseline recency weight
        Object lastMod = fragment.metadata().get("lastModified");
        if (lastMod instanceof Number) {
            score += 5.0;
        }

        return new RankingReason(RankingFactor.RECENCY, score, "Recency relevance score");
    }
}
