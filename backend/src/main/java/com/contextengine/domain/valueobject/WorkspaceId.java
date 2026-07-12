package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.Identifier;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the immutable, strongly typed unique identifier of a Workspace.
 * Enforces RFC-4122 Version 4 UUID rules.
 */
public record WorkspaceId(UUID value) implements Identifier {
    
    /**
     * Constructs a WorkspaceId and validates that the value is non-null and is a version 4 UUID.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a version 4 UUID
     */
    public WorkspaceId {
        Objects.requireNonNull(value, "WorkspaceId value must not be null");
        if (value.version() != 4) {
            throw new IllegalArgumentException("WorkspaceId UUID must be version 4");
        }
    }

    /**
     * Generates a new cryptographically secure WorkspaceId.
     *
     * @return a new WorkspaceId instance
     */
    public static WorkspaceId generate() {
        return new WorkspaceId(UUID.randomUUID());
    }

    /**
     * Parses a string representation of a UUID into a WorkspaceId.
     *
     * @param value the string representation of the UUID
     * @return a WorkspaceId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not conform to string UUID representation
     */
    public static WorkspaceId fromString(String value) {
        Objects.requireNonNull(value, "String value must not be null");
        return new WorkspaceId(UUID.fromString(value));
    }
}
