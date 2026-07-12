package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Bug.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record BugId(UUID value) implements Identifier {
    
    /**
     * Constructs a BugId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public BugId {
        Objects.requireNonNull(value, "BugId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("BugId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure BugId.
     *
     * @return a new BugId instance
     */
    public static BugId generate() {
        return new BugId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a BugId.
     *
     * @param value the string representation of the UUID
     * @return a BugId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static BugId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new BugId(UUID.fromString(value));
    }
}
