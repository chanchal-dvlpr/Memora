package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;
import java.util.Objects;

/**
 * Immutable model pairing a ContextFragment with its calculated RankingScore.
 */
public record RankedFragment(
    ContextFragment fragment,
    RankingScore score
) implements Comparable<RankedFragment> {

    public RankedFragment {
        Objects.requireNonNull(fragment, "fragment must not be null");
        Objects.requireNonNull(score, "score must not be null");
    }

    @Override
    public int compareTo(RankedFragment o) {
        Objects.requireNonNull(o, "RankedFragment must not be null");
        // Sort descending by score value
        int cmp = Double.compare(o.score().value(), this.score().value());
        if (cmp != 0) {
            return cmp;
        }
        // Lexicographical ordering fallback on fragment IDs to ensure strict determinism
        return this.fragment().fragmentId().compareTo(o.fragment().fragmentId());
    }
}
