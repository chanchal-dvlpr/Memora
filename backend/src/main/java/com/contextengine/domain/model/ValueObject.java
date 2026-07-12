package com.contextengine.domain.model;

/**
 * Marks an immutable domain value defined entirely by its structural state rather than identity.
 *
 * <p>Implementations should use structural equality and must not expose mutable state. This marker
 * reflects the Domain Model's Value Object pattern and remains independent of all frameworks.</p>
 */
public interface ValueObject {
}
