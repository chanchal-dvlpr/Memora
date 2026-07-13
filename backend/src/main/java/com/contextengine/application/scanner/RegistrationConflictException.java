package com.contextengine.application.scanner;

/**
 * Thrown when a parser registration conflicts with an already registered parser.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class RegistrationConflictException extends Exception {

    /**
     * Constructs a RegistrationConflictException.
     *
     * @param message diagnostic failure message
     */
    public RegistrationConflictException(String message) {
        super(message);
    }
}
