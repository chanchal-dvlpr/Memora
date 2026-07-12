package com.contextengine.domain.model;

import java.util.UUID;

/**
 * Represents the immutable, strongly typed UUID identity of a domain entity or aggregate root.
 *
 * <p>Concrete identifier value objects will implement this contract to prevent primitive UUID
 * usage across bounded contexts. The Domain Model requires each concrete identifier to preserve an
 * immutable UUID version 4 value for its entire lifecycle.</p>
 */
public interface Identifier extends ValueObject {

    /**
     * Returns the immutable UUID value of this identifier.
     *
     * @return the UUID value
     */
    UUID value();
}
