package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;

/**
 * Models platform-independent relative or absolute file paths.
 * Normalizes path separators to forward slashes.
 */
public record Path(String value) implements ValueObject, Comparable<Path> {
    
    /**
     * Constructs a normalized Path and rejects invalid filesystem characters or boundary escape attempts.
     *
     * @param value the raw path string
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if path contains invalid characters or escapes boundaries
     */
    public Path {
        Objects.requireNonNull(value, "Path value must not be null");
        
        // Normalize separators to standard forward slashes
        value = value.replace('\\', '/').trim();
        
        // Validate against invalid filesystem characters
        if (value.contains("*") || value.contains("?") || value.contains("<") || value.contains(">") || value.contains("|") || value.contains("\"")) {
            throw new IllegalArgumentException("Path contains invalid characters: " + value);
        }
        
        // Enforce the constraint: relative paths cannot escape parent project root boundaries
        if (value.startsWith("../") || value.contains("/../") || value.endsWith("/..") || value.equals("..")) {
            throw new IllegalArgumentException("Path attempts to escape project root boundary: " + value);
        }
    }

    /**
     * Returns whether this path is absolute.
     *
     * @return true if absolute, false otherwise
     */
    public boolean isAbsolute() {
        return value.startsWith("/");
    }

    /**
     * Returns whether this path is relative.
     *
     * @return true if relative, false otherwise
     */
    public boolean isRelative() {
        return !isAbsolute();
    }

    @Override
    public int compareTo(Path other) {
        return this.value.compareTo(other.value);
    }
}
