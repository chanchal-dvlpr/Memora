package com.contextengine.domain.valueobject;

/**
 * Specifies edge traversal directions relative to a focal Knowledge Node.
 */
public enum Direction {
    /** Outbound edges originating from the focal node. */
    OUTGOING,
    
    /** Inbound edges terminating at the focal node. */
    INCOMING,
    
    /** Both incoming and outgoing directed edges. */
    BOTH
}
