package com.contextengine.application.knowledge.context;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulation of context assembly outputs, statistics, and timestamp.
 */
public class ContextAssemblyResult {
    private final String projectId;
    private final List<ContextFragment> fragments;
    private final ContextStatistics statistics;
    private final Instant timestamp;

    public ContextAssemblyResult(
        String projectId,
        List<ContextFragment> fragments,
        ContextStatistics statistics,
        Instant timestamp
    ) {
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.fragments = fragments != null ? List.copyOf(fragments) : Collections.emptyList();
        this.statistics = Objects.requireNonNull(statistics, "statistics must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public String projectId() {
        return projectId;
    }

    public List<ContextFragment> fragments() {
        return fragments;
    }

    public ContextStatistics statistics() {
        return statistics;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
