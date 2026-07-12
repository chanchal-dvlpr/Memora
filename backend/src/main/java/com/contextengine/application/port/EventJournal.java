package com.contextengine.application.port;

import com.contextengine.application.event.UniversalEventFrame;
import java.util.List;
import java.util.UUID;

/**
 * Port interface for persisting and retrieving events from the persistent ledger.
 * Provides the storage contract to reconstruct indexes and support replay.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public interface EventJournal {

    /**
     * Appends an event to the persistent append-only ledger.
     *
     * @param envelope event frame to persist
     */
    void persist(UniversalEventFrame envelope);

    /**
     * Retrieves historical events chronologically from a starting sequence number.
     *
     * @param projectId project identifier, or null for system-wide events
     * @param startSequenceNum starting sequence number (inclusive)
     * @return chronological list of historical events
     */
    List<UniversalEventFrame> retrieveEvents(UUID projectId, Long startSequenceNum);

    /**
     * Gets the latest sequence number recorded for the project.
     *
     * @param projectId project identifier, or null for system-wide events
     * @return maximum sequence number, or 0 if no events exist
     */
    Long getLatestSequenceNum(UUID projectId);
}
