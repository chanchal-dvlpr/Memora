package com.contextengine.infrastructure.exception;

/**
 * Base exception class representing physical system-level, hardware, or third-party infrastructure failures.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: System Diagnostics
 * </p>
 */
public class InfrastructureException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an InfrastructureException with a detailed message.
     *
     * @param message detailed message explaining the error
     */
    public InfrastructureException(String message) {
        super(message);
    }

    /**
     * Constructs an InfrastructureException with a detailed message and a root cause.
     *
     * @param message detailed message explaining the error
     * @param cause the root cause of the error
     */
    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
