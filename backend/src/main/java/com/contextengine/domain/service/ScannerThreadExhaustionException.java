package com.contextengine.domain.service;

/**
 * Thrown when background scanner worker threads cannot be allocated.
 */
public class ScannerThreadExhaustionException extends RuntimeException {
    public ScannerThreadExhaustionException(String message) {
        super(message);
    }
}
