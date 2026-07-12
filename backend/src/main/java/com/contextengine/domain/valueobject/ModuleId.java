package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Module.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record ModuleId(UUID value) implements Identifier {
    
    /**
     * Constructs a ModuleId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public ModuleId {
        Objects.requireNonNull(value, "ModuleId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("ModuleId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure ModuleId.
     *
     * @return a new ModuleId instance
     */
    public static ModuleId generate() {
        return new ModuleId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a ModuleId.
     *
     * @param value the string representation of the UUID
     * @return a ModuleId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static ModuleId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new ModuleId(UUID.fromString(value));
    }
}
