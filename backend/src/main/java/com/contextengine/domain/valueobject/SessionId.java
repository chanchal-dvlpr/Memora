package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of an AI Session.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record SessionId(UUID value) implements Identifier {
    
    /**
     * Constructs a SessionId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public SessionId {
        Objects.requireNonNull(value, "SessionId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("SessionId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure SessionId.
     *
     * @return a new SessionId instance
     */
    public static SessionId generate() {
        return new SessionId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a SessionId.
     *
     * @param value the string representation of the UUID
     * @return a SessionId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static SessionId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new SessionId(UUID.fromString(value));
    }
}
