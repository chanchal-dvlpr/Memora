package com.contextengine.application.knowledge.ranking;

import java.util.Collections;
import java.util.List;

/**
 * Immutable scoring model capturing aggregate weight values and contribution reasons.
 */
public record RankingScore(
    double value,
    List<RankingReason> reasons
) {
    public RankingScore {
        reasons = reasons != null ? List.copyOf(reasons) : Collections.emptyList();
    }
}
