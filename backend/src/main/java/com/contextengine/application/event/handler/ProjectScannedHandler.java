package com.contextengine.application.event.handler;

import com.contextengine.application.event.UniversalEventFrame;
import java.util.Objects;

/**
 * Event handler coordinating operations triggered when a project codebase scan finishes.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class ProjectScannedHandler {

    /**
     * Handles scanner output results logging.
     *
     * @param envelope the event envelope
     */
    public void handle(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        System.out.printf("[EVENT-HANDLER] Project %s scanned. Files count: %s, Symbols count: %s%n",
            envelope.payload().get("projectId"),
            envelope.payload().get("filesScannedCount"),
            envelope.payload().get("symbolsCount")
        );
    }
}
