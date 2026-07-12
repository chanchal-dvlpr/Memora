package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Constraint.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record ConstraintId(UUID value) implements Identifier {
    
    /**
     * Constructs a ConstraintId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public ConstraintId {
        Objects.requireNonNull(value, "ConstraintId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("ConstraintId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure ConstraintId.
     *
     * @return a new ConstraintId instance
     */
    public static ConstraintId generate() {
        return new ConstraintId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a ConstraintId.
     *
     * @param value the string representation of the UUID
     * @return a ConstraintId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static ConstraintId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new ConstraintId(UUID.fromString(value));
    }
}
