package com.contextengine.application.event;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Validator responsible for verifying the integrity, metadata, and topic structure
 * of UniversalEventFrame envelopes before they are dispatched over the event bus.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class EventValidator {

    /**
     * Validates a UniversalEventFrame. Throws EventException if invalid.
     *
     * @param envelope event frame
     * @throws EventException if validation fails
     */
    public void validate(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");

        // Envelope Integrity Check
        if (envelope.eventId() == null) {
            throw new EventException("Event validation failed: eventId is null");
        }
        if (envelope.topic() == null || envelope.topic().trim().isEmpty()) {
            throw new EventException("Event validation failed: topic is null or empty");
        }
        if (envelope.timestamp() == null) {
            throw new EventException("Event validation failed: timestamp is null");
        }
        if (envelope.correlationId() == null) {
            throw new EventException("Event validation failed: correlationId is null");
        }
        if (envelope.causationId() == null) {
            throw new EventException("Event validation failed: causationId is null");
        }
        if (envelope.projectId() == null) {
            throw new EventException("Event validation failed: projectId is null");
        }
        if (envelope.version() == null || envelope.version() <= 0) {
            throw new EventException("Event validation failed: version must be positive");
        }

        // Topic Validation
        String topic = envelope.topic();
        if (!topic.contains(".p_")) {
            throw new EventException("Event validation failed: topic must contain project namespace prefix (.p_)");
        }

        String[] parts = topic.split("\\.p_");
        if (parts.length != 2) {
            throw new EventException("Event validation failed: topic has invalid project suffix format");
        }

        try {
            UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new EventException("Event validation failed: topic project suffix is not a valid UUID", e);
        }

        // Payload schema verification
        Map<String, Object> payload = envelope.payload();
        if (payload == null) {
            throw new EventException("Event validation failed: payload map is null");
        }
    }
}
