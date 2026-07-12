package com.contextengine.persistence.exception;

/**
 * Custom runtime exception representing errors in the persistence layer.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class PersistenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a PersistenceException with a message.
     *
     * @param message error description
     */
    public PersistenceException(String message) {
        super(message);
    }

    /**
     * Constructs a PersistenceException with message and cause.
     *
     * @param message error description
     * @param cause cause of the exception
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
