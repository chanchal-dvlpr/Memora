package com.contextengine.application.exception;

/**
 * Base exception class representing business scenario failures or validation issues in the application layer.
 */
public class ApplicationException extends RuntimeException {
    
    /**
     * Constructs a new ApplicationException with a detail message.
     *
     * @param message the failure details
     */
    public ApplicationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ApplicationException with a detail message and a causing exception.
     *
     * @param message the failure details
     * @param cause the original cause
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
