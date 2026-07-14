package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;

/**
 * Strategy interface representing individual ranking factors.
 */
public interface RankingStrategy {
    RankingReason evaluate(ContextFragment fragment, RankingConfiguration config);
}
