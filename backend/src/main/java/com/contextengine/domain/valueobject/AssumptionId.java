package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of an Assumption.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record AssumptionId(UUID value) implements Identifier {
    
    /**
     * Constructs an AssumptionId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public AssumptionId {
        Objects.requireNonNull(value, "AssumptionId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("AssumptionId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure AssumptionId.
     *
     * @return a new AssumptionId instance
     */
    public static AssumptionId generate() {
        return new AssumptionId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into an AssumptionId.
     *
     * @param value the string representation of the UUID
     * @return an AssumptionId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static AssumptionId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new AssumptionId(UUID.fromString(value));
    }
}
