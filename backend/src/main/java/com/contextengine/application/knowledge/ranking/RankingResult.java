package com.contextengine.application.knowledge.ranking;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Output envelope carrying deterministically ordered RankedFragments and stats.
 */
public class RankingResult {
    private final String projectId;
    private final List<RankedFragment> rankedFragments;
    private final RankingStatistics statistics;
    private final Instant timestamp;

    public RankingResult(
        String projectId,
        List<RankedFragment> rankedFragments,
        RankingStatistics statistics,
        Instant timestamp
    ) {
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.rankedFragments = rankedFragments != null ? List.copyOf(rankedFragments) : Collections.emptyList();
        this.statistics = Objects.requireNonNull(statistics, "statistics must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public String projectId() {
        return projectId;
    }

    public List<RankedFragment> rankedFragments() {
        return rankedFragments;
    }

    public RankingStatistics statistics() {
        return statistics;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
