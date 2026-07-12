package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of an Event.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record EventId(UUID value) implements Identifier {
    
    /**
     * Constructs an EventId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public EventId {
        Objects.requireNonNull(value, "EventId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("EventId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure EventId.
     *
     * @return a new EventId instance
     */
    public static EventId generate() {
        return new EventId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into an EventId.
     *
     * @param value the string representation of the UUID
     * @return an EventId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static EventId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new EventId(UUID.fromString(value));
    }
}
