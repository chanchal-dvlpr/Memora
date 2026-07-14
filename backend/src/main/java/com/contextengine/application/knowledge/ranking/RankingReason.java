package com.contextengine.application.knowledge.ranking;

import java.util.Objects;

/**
 * Descriptor capturing the contribution of a specific factor to the ranking score.
 */
public record RankingReason(
    RankingFactor factor,
    double contribution,
    String description
) {
    public RankingReason {
        Objects.requireNonNull(factor, "factor must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
