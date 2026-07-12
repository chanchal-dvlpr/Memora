package com.contextengine.domain.service;

/**
 * Thrown when an orphan relationship is detected or other integrity rules are violated.
 */
public class GraphIntegrityViolationException extends RuntimeException {
    public GraphIntegrityViolationException(String message) {
        super(message);
    }
}
