package com.contextengine.application.exception;

/**
 * Exception thrown when a requested context snapshot cannot be resolved in the repository history.
 * <p>
 * Bounded Context: Context Assembly
 * Reference: Section 5.5.7 (Snapshot Resources) error condition SNAPSHOT_NOT_FOUND
 * </p>
 */
public class SnapshotNotFoundException extends ApplicationException {

    /**
     * Constructs a SnapshotNotFoundException.
     *
     * @param message the detailed failure message
     */
    public SnapshotNotFoundException(String message) {
        super(message);
    }
}
