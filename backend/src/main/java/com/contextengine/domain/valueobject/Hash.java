package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stores cryptographic validation hashes (SHA-256) of physical file states to track mutations.
 */
public record Hash(String value) implements ValueObject {
    
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    /**
     * Constructs a Hash and validates that the value maps to a 64-character hexadecimal SHA-256 result.
     * Normalizes the hash value to lowercase to support case-insensitive equality.
     *
     * @param value the hexadecimal string representing the SHA-256 hash
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a valid 64-character hex string
     */
    public Hash {
        Objects.requireNonNull(value, "Hash value must not be null");
        if (!SHA256_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Hash value must be a valid 64-character hexadecimal string");
        }
        value = value.toLowerCase();
    }
}
