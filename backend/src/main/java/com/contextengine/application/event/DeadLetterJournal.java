package com.contextengine.application.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Journaling registry capturing and quarantining event frames that fail all processing retries.
 * Provides diagnostic hooks for system recovery.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class DeadLetterJournal {

    private final Queue<UniversalEventFrame> quarantine = new ConcurrentLinkedQueue<>();

    /**
     * Quarantines a failed event envelope.
     *
     * @param envelope event frame
     * @param reason failure reason
     */
    public void quarantine(UniversalEventFrame envelope, String reason) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        quarantine.add(envelope);
        System.err.printf("[DEAD-LETTER] Event ID %s on topic %s quarantined: %s%n",
            envelope.eventId(), envelope.topic(), reason);
    }

    /**
     * Returns all currently quarantined events.
     *
     * @return unmodifiable collection of event envelopes
     */
    public Collection<UniversalEventFrame> quarantinedEvents() {
        return Collections.unmodifiableCollection(quarantine);
    }

    /**
     * Clears all quarantined events.
     */
    public void clear() {
        quarantine.clear();
    }
}
