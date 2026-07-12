package com.contextengine.application.event;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable transaction wrapper representing a unified event envelope (Universal Event Frame).
 * Wraps all domain and platform-specific events for transport over the event bus.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public final class UniversalEventFrame {

    private final UUID eventId;
    private final String topic;
    private final Instant timestamp;
    private final UUID correlationId;
    private final UUID causationId;
    private final UUID projectId;
    private final Integer version;
    private final Map<String, Object> payload;

    /**
     * Constructs a UniversalEventFrame.
     *
     * @param eventId globally unique event identifier
     * @param topic hierarchical routing path
     * @param timestamp event occurrence instant
     * @param correlationId parent transaction tracing correlation identifier
     * @param causationId direct trigger event causation identifier
     * @param projectId associated project workspace identifier
     * @param version event schema version
     * @param payload event-specific payload values map
     */
    public UniversalEventFrame(
        UUID eventId,
        String topic,
        Instant timestamp,
        UUID correlationId,
        UUID causationId,
        UUID projectId,
        Integer version,
        Map<String, Object> payload
    ) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp must not be null");
        this.correlationId = Objects.requireNonNull(correlationId, "Correlation ID must not be null");
        this.causationId = Objects.requireNonNull(causationId, "Causation ID must not be null");
        this.projectId = Objects.requireNonNull(projectId, "Project ID must not be null");
        this.version = Objects.requireNonNull(version, "Version must not be null");
        Objects.requireNonNull(payload, "Payload must not be null");
        
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic must not be null or empty");
        }
        this.topic = topic.trim();
        this.payload = Map.copyOf(payload);
    }

    public UUID eventId() {
        return eventId;
    }

    public String topic() {
        return topic;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public UUID correlationId() {
        return correlationId;
    }

    public UUID causationId() {
        return causationId;
    }

    public UUID projectId() {
        return projectId;
    }

    public Integer version() {
        return version;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniversalEventFrame that)) return false;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }
}
