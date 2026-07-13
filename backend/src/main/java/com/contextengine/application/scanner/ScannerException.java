package com.contextengine.application.scanner;

/**
 * Custom runtime exception representing failures in the Workspace Scanner Subsystem.
 * Used for boundary confinements violations, symlink circular errors, and session lock exceptions.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class ScannerException extends RuntimeException {

    /**
     * Constructs a ScannerException with a custom message.
     *
     * @param message failure detail message
     */
    public ScannerException(String message) {
        super(message);
    }

    /**
     * Constructs a ScannerException with a custom message and a root cause.
     *
     * @param message failure detail message
     * @param cause underlying cause exception
     */
    public ScannerException(String message, Throwable cause) {
        super(message, cause);
    }
}
