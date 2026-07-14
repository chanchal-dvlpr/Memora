package com.contextengine.application.knowledge.context;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics metrics collected during a context assembly run.
 */
public class ContextStatistics {
    private final AtomicLong totalFragments = new AtomicLong(0);
    private final AtomicLong selectedFragments = new AtomicLong(0);
    private final AtomicLong filteredFragments = new AtomicLong(0);
    private final AtomicLong duplicateFragmentsRemoved = new AtomicLong(0);
    private final AtomicLong validationWarnings = new AtomicLong(0);
    private final AtomicLong skippedFragments = new AtomicLong(0);
    private final AtomicLong assemblyDuration = new AtomicLong(0);
    private final AtomicLong validationDuration = new AtomicLong(0);
    private final AtomicLong processingDuration = new AtomicLong(0);

    // Backward-compatibility aliases
    public long fragmentsCreated() {
        return totalFragments.get();
    }

    public void incrementFragmentsCreated(long val) {
        totalFragments.addAndGet(val);
    }

    public long warnings() {
        return validationWarnings.get();
    }

    public void incrementWarnings(long val) {
        validationWarnings.addAndGet(val);
    }

    public long skippedNodes() {
        return skippedFragments.get();
    }

    public void incrementSkippedNodes(long val) {
        skippedFragments.addAndGet(val);
    }

    // Extended Statistics Accessors
    public long totalFragments() {
        return totalFragments.get();
    }

    public void incrementTotalFragments(long val) {
        totalFragments.addAndGet(val);
    }

    public long selectedFragments() {
        return selectedFragments.get();
    }

    public void incrementSelectedFragments(long val) {
        selectedFragments.addAndGet(val);
    }

    public long filteredFragments() {
        return filteredFragments.get();
    }

    public void incrementFilteredFragments(long val) {
        filteredFragments.addAndGet(val);
    }

    public long duplicateFragmentsRemoved() {
        return duplicateFragmentsRemoved.get();
    }

    public void incrementDuplicateFragmentsRemoved(long val) {
        duplicateFragmentsRemoved.addAndGet(val);
    }

    public long validationWarnings() {
        return validationWarnings.get();
    }

    public void incrementValidationWarnings(long val) {
        validationWarnings.addAndGet(val);
    }

    public long skippedFragments() {
        return skippedFragments.get();
    }

    public void incrementSkippedFragments(long val) {
        skippedFragments.addAndGet(val);
    }

    public long assemblyDuration() {
        return assemblyDuration.get();
    }

    public void setAssemblyDuration(long val) {
        assemblyDuration.set(val);
    }

    public long validationDuration() {
        return validationDuration.get();
    }

    public void setValidationDuration(long val) {
        validationDuration.set(val);
    }

    public long processingDuration() {
        return processingDuration.get();
    }

    public void setProcessingDuration(long val) {
        processingDuration.set(val);
    }
}
