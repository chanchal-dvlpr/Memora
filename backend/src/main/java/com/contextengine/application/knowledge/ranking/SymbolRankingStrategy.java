package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;

/**
 * Strategy evaluating symbol relevance scores.
 */
public class SymbolRankingStrategy implements RankingStrategy {

    @Override
    public RankingReason evaluate(ContextFragment fragment, RankingConfiguration config) {
        if (fragment.fragmentType() == ContextFragmentType.SYMBOL) {
            double score = config.enableSymbolWeight() ? 60.0 : 15.0;
            return new RankingReason(RankingFactor.SYMBOL, score, "Symbol relevance score");
        }
        return new RankingReason(RankingFactor.SYMBOL, 0.0, "Not a symbol fragment");
    }
}
