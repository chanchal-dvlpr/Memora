package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores unstructured, extensible key-value metrics associated with entities.
 * Guarantees immutability and structural equivalence for value object comparison.
 */
public record Metadata(Map<String, String> values) implements ValueObject {
    
    /**
     * Constructs a Metadata value object. Performs defensive copying to ensure absolute immutability.
     *
     * @param values the key-value map
     * @throws NullPointerException if values map or any key/value inside is null
     */
    public Metadata {
        Objects.requireNonNull(values, "Metadata values must not be null");
        Map<String, String> copy = new HashMap<>(values);
        for (Map.Entry<String, String> entry : copy.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "Metadata keys must not be null");
            Objects.requireNonNull(entry.getValue(), "Metadata values must not be null");
        }
        values = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns an empty Metadata instance.
     *
     * @return empty metadata
     */
    public static Metadata empty() {
        return new Metadata(Collections.emptyMap());
    }

    /**
     * Returns the value associated with the given key, if present.
     *
     * @param key the metadata key to look up
     * @return the metadata value, or null if not found
     */
    public String get(String key) {
        return values.get(key);
    }
}
