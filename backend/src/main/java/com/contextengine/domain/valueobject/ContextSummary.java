package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.List;
import java.util.Objects;

/**
 * Captures compact summaries of compiled context contents to optimize processing.
 * Tracks total file count, token footprint, and key/primary entities.
 */
public record ContextSummary(
    int totalFileCount,
    int tokenFootprint,
    List<String> primaryEntities
) implements ValueObject {
    
    /**
     * Constructs a ContextSummary and validates that key tracking metrics are non-negative.
     *
     * @param totalFileCount the total count of workspace files
     * @param tokenFootprint the token size footprint of the context
     * @param primaryEntities the key domain entities included in the context
     * @throws IllegalArgumentException if totalFileCount or tokenFootprint is negative
     * @throws NullPointerException if primaryEntities list or any element is null
     */
    public ContextSummary {
        Objects.requireNonNull(primaryEntities, "Primary entities must not be null");
        if (totalFileCount < 0) {
            throw new IllegalArgumentException("Total file count must be non-negative");
        }
        if (tokenFootprint < 0) {
            throw new IllegalArgumentException("Token footprint must be non-negative");
        }
        primaryEntities = List.copyOf(primaryEntities);
    }
}
