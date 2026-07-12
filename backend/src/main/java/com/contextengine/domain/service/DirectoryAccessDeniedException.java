package com.contextengine.domain.service;

/**
 * Thrown when write or read permissions are absent for a target directory.
 */
public class DirectoryAccessDeniedException extends RuntimeException {
    public DirectoryAccessDeniedException(String message) {
        super(message);
    }
}
