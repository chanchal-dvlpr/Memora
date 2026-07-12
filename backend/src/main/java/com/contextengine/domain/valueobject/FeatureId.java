package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Feature.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record FeatureId(UUID value) implements Identifier {
    
    /**
     * Constructs a FeatureId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public FeatureId {
        Objects.requireNonNull(value, "FeatureId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("FeatureId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure FeatureId.
     *
     * @return a new FeatureId instance
     */
    public static FeatureId generate() {
        return new FeatureId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a FeatureId.
     *
     * @param value the string representation of the UUID
     * @return a FeatureId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static FeatureId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new FeatureId(UUID.fromString(value));
    }
}
