package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Project.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record ProjectId(UUID value) implements Identifier {
    
    /**
     * Constructs a ProjectId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public ProjectId {
        Objects.requireNonNull(value, "ProjectId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("ProjectId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure ProjectId.
     *
     * @return a new ProjectId instance
     */
    public static ProjectId generate() {
        return new ProjectId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a ProjectId.
     *
     * @param value the string representation of the UUID
     * @return a ProjectId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static ProjectId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new ProjectId(UUID.fromString(value));
    }
}
