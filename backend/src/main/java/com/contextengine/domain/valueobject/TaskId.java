package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Task.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record TaskId(UUID value) implements Identifier {
    
    /**
     * Constructs a TaskId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public TaskId {
        Objects.requireNonNull(value, "TaskId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("TaskId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure TaskId.
     *
     * @return a new TaskId instance
     */
    public static TaskId generate() {
        return new TaskId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a TaskId.
     *
     * @param value the string representation of the UUID
     * @return a TaskId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static TaskId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new TaskId(UUID.fromString(value));
    }
}
