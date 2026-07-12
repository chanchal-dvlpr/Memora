package com.contextengine.domain.service;

/**
 * Thrown when index structures are corrupted or out of sync.
 */
public class IndexOutOfSyncException extends RuntimeException {
    public IndexOutOfSyncException(String message) {
        super(message);
    }
}
