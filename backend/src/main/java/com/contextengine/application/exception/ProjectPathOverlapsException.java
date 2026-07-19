package com.contextengine.application.exception;

/**
 * Thrown when registering a project whose root directory path overlaps with an existing registered project.
 */
public class ProjectPathOverlapsException extends ApplicationException {
    
    public ProjectPathOverlapsException(String message) {
        super(message);
    }

    public ProjectPathOverlapsException(String message, Throwable cause) {
        super(message, cause);
    }
}
