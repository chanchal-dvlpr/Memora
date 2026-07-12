package com.contextengine.domain.aggregate;

import com.contextengine.domain.model.Identifier;

/**
 * Marks the exclusive external entry point for a transactional domain aggregate.
 *
 * <p>An aggregate root owns invariant enforcement and is the only domain object outside callers may
 * reference to mutate aggregate state. Child entities and value objects remain inside the aggregate
 * boundary, as required by the Domain Model.</p>
 *
 * @param <I> the strongly typed immutable identifier of the aggregate root
 */
public interface AggregateRoot<I extends Identifier> {

    /**
     * Returns the immutable identity of this aggregate root.
     *
     * @return the aggregate root identifier
     */
    I id();
}
