package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;

/**
 * Strategy evaluating dependency relevance scores.
 */
public class DependencyRankingStrategy implements RankingStrategy {

    @Override
    public RankingReason evaluate(ContextFragment fragment, RankingConfiguration config) {
        if (fragment.fragmentType() == ContextFragmentType.DEPENDENCY) {
            double score = config.enableDependencyWeight() ? 50.0 : 10.0;
            return new RankingReason(RankingFactor.DEPENDENCY, score, "Dependency relevance score");
        }
        return new RankingReason(RankingFactor.DEPENDENCY, 0.0, "Not a dependency fragment");
    }
}
