package com.contextengine.domain.service;

/**
 * Thrown when ADR metadata parameters are missing or formatted incorrectly.
 */
public class InvalidADRFormatException extends RuntimeException {
    public InvalidADRFormatException(String message) {
        super(message);
    }
}
