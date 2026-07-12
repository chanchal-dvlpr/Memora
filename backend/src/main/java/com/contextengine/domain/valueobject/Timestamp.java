package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Captures high-precision, monotonic UTC timestamps for chronological ordering.
 * Conforms to ISO-8601 formatting with microsecond precision.
 */
public record Timestamp(Instant value) implements ValueObject, Comparable<Timestamp> {
    
    /**
     * Constructs a Timestamp and truncates the internal Instant to microsecond precision.
     *
     * @param value the UTC Instant value
     * @throws NullPointerException if value is null
     */
    public Timestamp {
        Objects.requireNonNull(value, "Timestamp value must not be null");
        value = value.truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Captures the current system UTC instant as a Timestamp.
     *
     * @return the current Timestamp
     */
    public static Timestamp now() {
        return new Timestamp(Instant.now());
    }

    /**
     * Parses an ISO-8601 formatted string representation of a timestamp.
     *
     * @param text the ISO-8601 string
     * @return a parsed Timestamp instance
     * @throws NullPointerException if text is null
     */
    public static Timestamp parse(String text) {
        Objects.requireNonNull(text, "Text must not be null");
        return new Timestamp(Instant.parse(text));
    }

    /**
     * Formats the timestamp into a standard ISO-8601 string representation.
     *
     * @return the formatted ISO-8601 string
     */
    public String toIsoString() {
        return DateTimeFormatter.ISO_INSTANT.format(value);
    }

    @Override
    public int compareTo(Timestamp other) {
        return this.value.compareTo(other.value);
    }
}
