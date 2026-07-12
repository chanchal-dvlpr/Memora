package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Relationship (edge) in the Knowledge Graph.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record RelationshipId(UUID value) implements Identifier {
    
    /**
     * Constructs a RelationshipId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public RelationshipId {
        Objects.requireNonNull(value, "RelationshipId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("RelationshipId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure RelationshipId.
     *
     * @return a new RelationshipId instance
     */
    public static RelationshipId generate() {
        return new RelationshipId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a RelationshipId.
     *
     * @param value the string representation of the UUID
     * @return a RelationshipId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static RelationshipId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new RelationshipId(UUID.fromString(value));
    }
}
