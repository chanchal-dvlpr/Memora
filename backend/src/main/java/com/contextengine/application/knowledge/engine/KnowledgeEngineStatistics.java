package com.contextengine.application.knowledge.engine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics tracking for Knowledge Engine operations.
 */
public class KnowledgeEngineStatistics {

    private final AtomicLong nodesProcessed = new AtomicLong(0);
    private final AtomicLong relationshipsProcessed = new AtomicLong(0);
    private final AtomicLong processingDurationMs = new AtomicLong(0);
    private final AtomicLong warnings = new AtomicLong(0);
    private final AtomicLong skippedEntities = new AtomicLong(0);

    public long nodesProcessed() {
        return nodesProcessed.get();
    }

    public long relationshipsProcessed() {
        return relationshipsProcessed.get();
    }

    public long processingDurationMs() {
        return processingDurationMs.get();
    }

    public long warnings() {
        return warnings.get();
    }

    public long skippedEntities() {
        return skippedEntities.get();
    }

    public void incrementNodesProcessed(long delta) {
        nodesProcessed.addAndGet(delta);
    }

    public void incrementRelationshipsProcessed(long delta) {
        relationshipsProcessed.addAndGet(delta);
    }

    public void setProcessingDurationMs(long duration) {
        processingDurationMs.set(duration);
    }

    public void incrementWarnings(long delta) {
        warnings.addAndGet(delta);
    }

    public void incrementSkippedEntities(long delta) {
        skippedEntities.addAndGet(delta);
    }
}
