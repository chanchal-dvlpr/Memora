package com.contextengine.infrastructure.event;

/**
 * Defines standardized event topics for the Project Scanner lifecycle.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public final class ScannerEventTopics {

    /** Topic published when a directory scan starts. */
    public static final String SCAN_STARTED = "engine.project.lifecycle.scan_started";

    /** Topic published when a directory scan successfully completes. */
    public static final String SCAN_COMPLETED = "engine.project.lifecycle.scan_completed";

    private ScannerEventTopics() {
    }
}
