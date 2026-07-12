package com.contextengine.domain.service;

/**
 * Thrown when event loop queue exceeds throughput limits.
 */
public class EventQueueOverflowException extends RuntimeException {
    public EventQueueOverflowException(String message) {
        super(message);
    }
}
