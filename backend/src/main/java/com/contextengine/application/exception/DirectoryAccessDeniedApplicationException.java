package com.contextengine.application.exception;

/**
 * Thrown when target workspace directory cannot be accessed or lacks write permissions.
 */
public class DirectoryAccessDeniedApplicationException extends ApplicationException {
    
    public DirectoryAccessDeniedApplicationException(String message) {
        super(message);
    }

    public DirectoryAccessDeniedApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
