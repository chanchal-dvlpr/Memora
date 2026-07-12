package com.contextengine.domain.service;

/**
 * Thrown when the database is locked or corrupted, preventing graph access.
 */
public class GraphUnreachableException extends RuntimeException {
    public GraphUnreachableException(String message) {
        super(message);
    }
}
