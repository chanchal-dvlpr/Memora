package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;

/**
 * Strategy evaluating structural containment importance based on fragment type.
 */
public class StructuralRankingStrategy implements RankingStrategy {

    @Override
    public RankingReason evaluate(ContextFragment fragment, RankingConfiguration config) {
        if (!config.enableStructuralWeight()) {
            return new RankingReason(RankingFactor.STRUCTURAL_IMPORTANCE, 0.0, "Structural weights disabled");
        }

        double score = switch (fragment.fragmentType()) {
            case PROJECT -> 100.0;
            case WORKSPACE -> 95.0;
            case MODULE -> 90.0;
            case DIRECTORY -> 80.0;
            case FILE -> 70.0;
            default -> 0.0;
        };

        return new RankingReason(RankingFactor.STRUCTURAL_IMPORTANCE, score, "Base structural importance score");
    }
}
