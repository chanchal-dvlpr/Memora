package com.contextengine.domain.service;

/**
 * Thrown when target sequence markers are missing during snapshot rollbacks.
 */
public class VersionNotFoundException extends RuntimeException {
    public VersionNotFoundException(String message) {
        super(message);
    }
}
