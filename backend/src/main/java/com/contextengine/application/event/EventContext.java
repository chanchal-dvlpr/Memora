package com.contextengine.application.event;

import java.util.UUID;

/**
 * Thread-local context tracking correlation and causation identifiers for event-chain lineage.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public final class EventContext {

    private static final ThreadLocal<UUID> correlationId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> causationId = new ThreadLocal<>();

    private EventContext() {
        // Prevent instantiation
    }

    /**
     * Retrieves the correlation identifier for the current thread.
     *
     * @return the correlation UUID, or a new random UUID if none exists
     */
    public static UUID correlationId() {
        UUID id = correlationId.get();
        if (id == null) {
            id = UUID.randomUUID();
            correlationId.set(id);
        }
        return id;
    }

    /**
     * Sets the correlation identifier for the current thread.
     *
     * @param id correlation UUID
     */
    public static void setCorrelationId(UUID id) {
        correlationId.set(id);
    }

    /**
     * Retrieves the causation identifier for the current thread.
     *
     * @return the causation UUID, or a new random UUID if none exists
     */
    public static UUID causationId() {
        UUID id = causationId.get();
        if (id == null) {
            id = UUID.randomUUID();
            causationId.set(id);
        }
        return id;
    }

    /**
     * Sets the causation identifier for the current thread.
     *
     * @param id causation UUID
     */
    public static void setCausationId(UUID id) {
        causationId.set(id);
    }

    /**
     * Clears thread-local tracing identifiers.
     */
    public static void clear() {
        correlationId.remove();
        causationId.remove();
    }
}
