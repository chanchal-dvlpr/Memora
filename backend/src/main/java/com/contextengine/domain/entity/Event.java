package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.EventId;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.Timestamp;
import java.util.Objects;

/**
 * Represents an immutable record of an occurrence in the system domain.
 */
public class Event {
    
    private final EventId id;
    private final String type;
    private final Timestamp occurredAt;
    private final Metadata payload;

    /**
     * Constructs an Event.
     *
     * @param id the unique event ID
     * @param type the type of event (e.g. "ProjectRegistered")
     * @param occurredAt the high-precision timestamp when the event occurred
     * @param payload the metadata payload of the event
     */
    public Event(EventId id, String type, Timestamp occurredAt, Metadata payload) {
        this.id = Objects.requireNonNull(id, "EventId must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "OccurredAt timestamp must not be null");
        this.payload = Objects.requireNonNull(payload, "Payload must not be null");
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type must not be null or empty");
        }
        this.type = type.trim();
    }

    public EventId id() {
        return id;
    }

    public String type() {
        return type;
    }

    public Timestamp occurredAt() {
        return occurredAt;
    }

    public Metadata payload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
