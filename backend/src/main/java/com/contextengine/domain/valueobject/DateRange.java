package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;

/**
 * Represents a chronological window bounded by start and end timestamps.
 */
public record DateRange(Timestamp start, Timestamp end) implements ValueObject {
    
    /**
     * Constructs a DateRange and validates that the start timestamp precedes or equals the end timestamp.
     *
     * @param start the start timestamp of the range
     * @param end the end timestamp of the range
     * @throws NullPointerException if start or end is null
     * @throws IllegalArgumentException if start timestamp is after end timestamp
     */
    public DateRange {
        Objects.requireNonNull(start, "Start timestamp must not be null");
        Objects.requireNonNull(end, "End timestamp must not be null");
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("Start timestamp must precede or equal the end timestamp");
        }
    }

    /**
     * Checks if the specified timestamp falls within this chronological window (inclusive).
     *
     * @param timestamp the timestamp to check
     * @return true if the timestamp falls within this window, false otherwise
     * @throws NullPointerException if timestamp is null
     */
    public boolean contains(Timestamp timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp to check must not be null");
        return start.compareTo(timestamp) <= 0 && end.compareTo(timestamp) >= 0;
    }
}
