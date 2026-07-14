package com.contextengine.application.knowledge.ranking;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics collector for the Relevance Ranking Engine.
 */
public class RankingStatistics {
    private final AtomicLong fragmentsProcessed = new AtomicLong(0);
    private final AtomicLong fragmentsRanked = new AtomicLong(0);
    private final AtomicLong rankingDuration = new AtomicLong(0);
    private final AtomicLong warnings = new AtomicLong(0);
    private final AtomicLong skippedFragments = new AtomicLong(0);

    private final AtomicLong validationDuration = new AtomicLong(0);
    private final AtomicLong averageScoreBits = new AtomicLong(Double.doubleToRawLongBits(0.0));
    private final AtomicLong highestScoreBits = new AtomicLong(Double.doubleToRawLongBits(0.0));
    private final AtomicLong lowestScoreBits = new AtomicLong(Double.doubleToRawLongBits(0.0));

    public long fragmentsProcessed() {
        return fragmentsProcessed.get();
    }

    public void incrementFragmentsProcessed(long val) {
        fragmentsProcessed.addAndGet(val);
    }

    public long fragmentsRanked() {
        return fragmentsRanked.get();
    }

    public void incrementFragmentsRanked(long val) {
        fragmentsRanked.addAndGet(val);
    }

    public long rankingDuration() {
        return rankingDuration.get();
    }

    public void setRankingDuration(long val) {
        rankingDuration.set(val);
    }

    public long warnings() {
        return warnings.get();
    }

    public void incrementWarnings(long val) {
        warnings.addAndGet(val);
    }

    public long skippedFragments() {
        return skippedFragments.get();
    }

    public void incrementSkippedFragments(long val) {
        skippedFragments.addAndGet(val);
    }

    public long validationDuration() {
        return validationDuration.get();
    }

    public void setValidationDuration(long val) {
        validationDuration.set(val);
    }

    public double averageScore() {
        return Double.longBitsToDouble(averageScoreBits.get());
    }

    public void setAverageScore(double val) {
        averageScoreBits.set(Double.doubleToRawLongBits(val));
    }

    public double highestScore() {
        return Double.longBitsToDouble(highestScoreBits.get());
    }

    public void setHighestScore(double val) {
        highestScoreBits.set(Double.doubleToRawLongBits(val));
    }

    public double lowestScore() {
        return Double.longBitsToDouble(lowestScoreBits.get());
    }

    public void setLowestScore(double val) {
        lowestScoreBits.set(Double.doubleToRawLongBits(val));
    }
}
