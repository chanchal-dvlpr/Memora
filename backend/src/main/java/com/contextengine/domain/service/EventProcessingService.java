package com.contextengine.domain.service;

import com.contextengine.domain.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Operates the asynchronous prioritized loop that serializes and dispatches internal system, git, and file events.
 */
public class EventProcessingService {
    
    private static final int QUEUE_LIMIT = 1000;
    private final List<DomainEvent> eventQueue = new ArrayList<>();

    /**
     * Queues a domain event for serialization and dispatching.
     * Enforces queue capacity limits.
     *
     * @param event the domain event occurrence
     * @throws EventQueueOverflowException if the queue has reached its capacity limit
     * @throws NullPointerException if event is null
     */
    public synchronized void queueEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Event must not be null");
        if (eventQueue.size() >= QUEUE_LIMIT) {
            throw new EventQueueOverflowException("Event processing queue limit exceeded. Capacity limit is " + QUEUE_LIMIT);
        }
        eventQueue.add(event);
    }

    /**
     * Dispatches and processes all currently queued domain events, then clears the queue.
     *
     * @return list of processed events
     */
    public synchronized List<DomainEvent> dispatchEvents() {
        List<DomainEvent> dispatched = new ArrayList<>(eventQueue);
        eventQueue.clear();
        return dispatched;
    }
}
