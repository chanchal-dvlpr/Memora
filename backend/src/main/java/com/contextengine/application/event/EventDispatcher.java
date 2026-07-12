package com.contextengine.application.event;

/**
 * Interface defining dispatching and subscription management in the Event Bus.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public interface EventDispatcher {

    /**
     * Routes and publishes the event envelope to all registered matching subscribers.
     *
     * @param envelope the event envelope to dispatch
     */
    void dispatch(UniversalEventFrame envelope);

    /**
     * Registers an event subscriber for a topic pattern.
     * Topic patterns support dot-separated notation with '*' single-segment wildcards.
     *
     * @param topicPattern hierarchical routing pattern (e.g. "workspace.file.*")
     * @param subscriber the subscriber callback to register
     */
    void subscribe(String topicPattern, EventSubscriber subscriber);

    /**
     * Unregisters an event subscriber from a topic pattern.
     *
     * @param topicPattern registered pattern
     * @param subscriber subscriber callback instance to remove
     */
    void unsubscribe(String topicPattern, EventSubscriber subscriber);
}
