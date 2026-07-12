package com.contextengine.domain.service;

/**
 * Thrown when AI handoff compilation fails due to corruption or accessibility.
 */
public class HandoffGenerationException extends RuntimeException {
    public HandoffGenerationException(String message) {
        super(message);
    }
}
