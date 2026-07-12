package com.contextengine.application.event.handler;

import com.contextengine.application.event.UniversalEventFrame;
import java.util.Objects;

/**
 * Event handler coordinating operations triggered when a codebase feature is created.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class FeatureCreatedHandler {

    /**
     * Handles feature registration logging.
     *
     * @param envelope the event envelope
     */
    public void handle(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        System.out.printf("[EVENT-HANDLER] Feature %s ('%s') created for project %s.%n",
            envelope.payload().get("featureId"),
            envelope.payload().get("title"),
            envelope.payload().get("projectId")
        );
    }
}
