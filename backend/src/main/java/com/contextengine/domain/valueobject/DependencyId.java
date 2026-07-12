package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Dependency.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record DependencyId(UUID value) implements Identifier {
    
    /**
     * Constructs a DependencyId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public DependencyId {
        Objects.requireNonNull(value, "DependencyId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("DependencyId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure DependencyId.
     *
     * @return a new DependencyId instance
     */
    public static DependencyId generate() {
        return new DependencyId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a DependencyId.
     *
     * @param value the string representation of the UUID
     * @return a DependencyId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static DependencyId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new DependencyId(UUID.fromString(value));
    }
}
