package com.contextengine.domain.service;

/**
 * Thrown when registering a project whose root directory overlaps or nests within an existing project.
 */
public class OverlappingProjectException extends RuntimeException {
    public OverlappingProjectException(String message) {
        super(message);
    }
}
