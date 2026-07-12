package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a standard semantic version conforming to SemVer 2.0.0 rules.
 */
public record SemanticVersion(String value) implements ValueObject, Comparable<SemanticVersion> {
    
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
        "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-]\\d*|[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-]\\d*|[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
        "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
    );

    /**
     * Constructs a SemanticVersion and validates it against SemVer 2.0.0 rules.
     *
     * @param value the semantic version string
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value does not match the SemVer pattern
     */
    public SemanticVersion {
        Objects.requireNonNull(value, "SemanticVersion value must not be null");
        if (!SEMVER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("SemanticVersion value '" + value + "' does not conform to SemVer 2.0.0 schema");
        }
    }

    @Override
    public int compareTo(SemanticVersion other) {
        return parseAndCompare(this.value, other.value);
    }

    private static int parseAndCompare(String v1, String v2) {
        String[] parts1 = v1.split("[\\.\\-\\+]");
        String[] parts2 = v2.split("[\\.\\-\\+]");
        int minLength = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < minLength; i++) {
            String p1 = parts1[i];
            String p2 = parts2[i];
            try {
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                int comp = Integer.compare(n1, n2);
                if (comp != 0) return comp;
            } catch (NumberFormatException e) {
                int comp = p1.compareTo(p2);
                if (comp != 0) return comp;
            }
        }
        return Integer.compare(parts1.length, parts2.length);
    }
}
