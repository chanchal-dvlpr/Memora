package com.contextengine.application.event.handler;

import com.contextengine.application.event.UniversalEventFrame;
import java.util.Objects;

/**
 * Event handler coordinating operations triggered when the Knowledge Graph commits updates.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class KnowledgeGraphUpdatedHandler {

    /**
     * Handles knowledge graph update logging.
     *
     * @param envelope the event envelope
     */
    public void handle(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        System.out.printf("[EVENT-HANDLER] Knowledge Graph updated for project %s. Nodes added: %s, Edges added: %s%n",
            envelope.payload().get("projectId"),
            envelope.payload().get("nodesAddedCount"),
            envelope.payload().get("edgesAddedCount")
        );
    }
}
