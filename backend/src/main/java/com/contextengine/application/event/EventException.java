package com.contextengine.application.event;

/**
 * Base runtime exception representing Event System failure occurrences.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class EventException extends RuntimeException {

    /**
     * Constructs an EventException with a message.
     *
     * @param message error details
     */
    public EventException(String message) {
        super(message);
    }

    /**
     * Constructs an EventException with a message and cause.
     *
     * @param message error details
     * @param cause underlying trigger cause
     */
    public EventException(String message, Throwable cause) {
        super(message, cause);
    }
}
