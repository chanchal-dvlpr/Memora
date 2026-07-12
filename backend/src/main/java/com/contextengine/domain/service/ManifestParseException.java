package com.contextengine.domain.service;

/**
 * Thrown when dependency manifest parsing fails.
 */
public class ManifestParseException extends RuntimeException {
    public ManifestParseException(String message) {
        super(message);
    }
}
