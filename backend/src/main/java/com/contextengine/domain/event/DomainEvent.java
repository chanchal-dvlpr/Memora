package com.contextengine.domain.event;

import java.time.Instant;

/**
 * Represents an immutable historical occurrence within the domain.
 *
 * <p>Concrete events describe completed facts in past tense and coordinate state changes across
 * aggregate boundaries. Event payloads, timestamps, and metadata must remain immutable after
 * publication, in accordance with the Domain Model.</p>
 */
public interface DomainEvent {

    /**
     * Returns the high-precision UTC instant at which the domain occurrence happened.
     *
     * @return the immutable occurrence timestamp
     */
    Instant occurredAt();
}
