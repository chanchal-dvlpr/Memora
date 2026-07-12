package com.contextengine.application.event;

/**
 * Functional interface implemented by event handlers/subscribers to receive event envelopes.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
@FunctionalInterface
public interface EventSubscriber {

    /**
     * Callback method invoked when a matching event is dispatched.
     *
     * @param envelope the universal event frame containing metadata and payload
     */
    void onEvent(UniversalEventFrame envelope);
}
