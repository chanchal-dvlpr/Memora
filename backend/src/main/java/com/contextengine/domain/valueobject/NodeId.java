package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Node in the Knowledge Graph.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record NodeId(UUID value) implements Identifier {
    
    /**
     * Constructs a NodeId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public NodeId {
        Objects.requireNonNull(value, "NodeId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("NodeId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure NodeId.
     *
     * @return a new NodeId instance
     */
    public static NodeId generate() {
        return new NodeId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a NodeId.
     *
     * @param value the string representation of the UUID
     * @return a NodeId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static NodeId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new NodeId(UUID.fromString(value));
    }
}
