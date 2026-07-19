package com.contextengine.application.exception;

/**
 * Thrown when attempting to register a project that has already been registered.
 */
public class ProjectAlreadyRegisteredException extends ApplicationException {
    
    public ProjectAlreadyRegisteredException(String message) {
        super(message);
    }

    public ProjectAlreadyRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }
}
