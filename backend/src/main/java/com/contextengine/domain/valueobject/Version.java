package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;

/**
 * Models sequence revisions, tracking historical states of context snapshots.
 * Represents a positive, incrementing integer starting from 1.
 */
public record Version(int value) implements ValueObject, Comparable<Version> {
    
    /**
     * Constructs a Version and validates that the value is positive (>= 1).
     *
     * @param value the version number
     * @throws IllegalArgumentException if value is less than 1
     */
    public Version {
        if (value < 1) {
            throw new IllegalArgumentException("Version value must be a positive integer starting from 1");
        }
    }

    /**
     * Creates a new Version representing the next sequential increment.
     *
     * @return the next version
     */
    public Version next() {
        return new Version(this.value + 1);
    }

    @Override
    public int compareTo(Version other) {
        return Integer.compare(this.value, other.value);
    }
}
