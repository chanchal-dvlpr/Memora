package com.contextengine.application.exception;

/**
 * Exception thrown when a requested project cannot be located in the repository.
 * <p>
 * Bounded Context: Project Management
 * Reference: Section 5.5.1 (Project Registry Engine) error condition PROJECT_NOT_FOUND
 * </p>
 */
public class ProjectNotFoundException extends ApplicationException {

    /**
     * Constructs a ProjectNotFoundException.
     *
     * @param message the detailed failure message
     */
    public ProjectNotFoundException(String message) {
        super(message);
    }
}
