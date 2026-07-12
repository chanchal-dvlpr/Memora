package com.contextengine.application.event;

import com.contextengine.application.port.EventJournal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service coordinating the replay of historical persisted events from the Event Journal.
 * Chronologically streams events back through the Local Event Bus dispatcher.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class EventReplayService {

    private final EventJournal eventJournal;
    private final EventDispatcher eventDispatcher;
    private final EventMonitor eventMonitor;

    /**
     * Constructs the EventReplayService.
     *
     * @param eventJournal persistent event log store port
     * @param eventDispatcher central event dispatcher
     * @param eventMonitor telemetry metrics monitor
     */
    public EventReplayService(EventJournal eventJournal, EventDispatcher eventDispatcher, EventMonitor eventMonitor) {
        this.eventJournal = Objects.requireNonNull(eventJournal, "EventJournal must not be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "EventDispatcher must not be null");
        this.eventMonitor = Objects.requireNonNull(eventMonitor, "EventMonitor must not be null");
    }

    /**
     * Replays persisted event frames sequentially starting from a sequence number.
     *
     * @param projectId project identifier, or null for system-wide events
     * @param startSequenceNum starting sequence offset (inclusive)
     */
    public void replay(UUID projectId, Long startSequenceNum) {
        List<UniversalEventFrame> history = eventJournal.retrieveEvents(projectId, startSequenceNum);
        for (UniversalEventFrame frame : history) {
            eventMonitor.recordReplay();
            eventDispatcher.dispatch(frame);
        }
    }

    /**
     * Replays all historical events starting from sequence number 1.
     *
     * @param projectId project identifier, or null for system-wide events
     */
    public void replayAll(UUID projectId) {
        replay(projectId, 1L);
    }
}
