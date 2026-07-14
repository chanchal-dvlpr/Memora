package com.contextengine.application.knowledge.graph;

/**
 * Utility tracker mapping total node and edge counts, build timings, and validation warnings in a KnowledgeGraph.
 */
public class GraphStatistics {

    private long nodeCount = 0;
    private long relationshipCount = 0;
    private long buildDurationMs = 0;
    private long validationDurationMs = 0;
    private long warningCount = 0;
    private long skippedEntities = 0;

    public long nodeCount() {
        return nodeCount;
    }

    public void setNodeCount(long count) {
        this.nodeCount = count;
    }

    public long relationshipCount() {
        return relationshipCount;
    }

    public void setRelationshipCount(long count) {
        this.relationshipCount = count;
    }

    public long buildDurationMs() {
        return buildDurationMs;
    }

    public void setBuildDurationMs(long buildDurationMs) {
        this.buildDurationMs = buildDurationMs;
    }

    public long validationDurationMs() {
        return validationDurationMs;
    }

    public void setValidationDurationMs(long validationDurationMs) {
        this.validationDurationMs = validationDurationMs;
    }

    public long warningCount() {
        return warningCount;
    }

    public void setWarningCount(long warningCount) {
        this.warningCount = warningCount;
    }

    public void incrementWarningCount(long delta) {
        this.warningCount += delta;
    }

    public long skippedEntities() {
        return skippedEntities;
    }

    public void setSkippedEntities(long skippedEntities) {
        this.skippedEntities = skippedEntities;
    }

    public void incrementSkippedEntities(long delta) {
        this.skippedEntities += delta;
    }
}
