package com.contextengine.domain.entity;

/**
 * Represents the connection and operational states of an AI Session.
 */
public enum SessionState {
    /** Negotiating client protocol parameters and handshake tokens. */
    HANDSHAKE,
    
    /** Actively processing queries and managing conversational context. */
    ACTIVE,
    
    /** Session terminated; temporary memory buffers purged. */
    CLOSED
}
