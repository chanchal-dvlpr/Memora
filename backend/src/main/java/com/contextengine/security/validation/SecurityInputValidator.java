package com.contextengine.security.validation;

import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import java.util.regex.Pattern;

/**
 * Service executing security-sensitive input sanitizations and parameter format validations.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Input Validation)
 * Responsibility: Reject malformed paths, enforce strict UUID patterns on identifiers, and sanitize user-controlled strings.
 * Dependencies: {@link SecurityConstants}, {@link SecurityException}
 * Future Usage: Utilized by controllers, gateways, and file service layers to sanitize data at the system boundary.
 * </p>
 */
public class SecurityInputValidator {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"
    );

    private static final Pattern TRAVERSAL_PATTERN = Pattern.compile(
        "\\.\\.(?:/|\\\\|\\z)"
    );

    /**
     * Validates that an identifier matches the strict UUID format.
     *
     * @param id the identifier string to check
     * @throws SecurityException if the identifier is null or has an invalid format
     */
    public void validateIdentifier(String id) {
        if (id == null || !UUID_PATTERN.matcher(id).matches()) {
            throw new SecurityException(
                SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                "Input validation breach: Malformed system identifier (must be a valid UUID)"
            );
        }
    }

    /**
     * Validates that a path string does not attempt traversal or contain forbidden characters.
     *
     * @param path the path string to check
     * @throws SecurityException if the path is empty, malformed, or attempts traversal
     */
    public void validatePathString(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new SecurityException(
                SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                "Input validation breach: Target path must not be null or empty"
            );
        }

        if (TRAVERSAL_PATTERN.matcher(path).find()) {
            throw new SecurityException(
                SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                "Input validation breach: Directory traversal sequence detected in path: " + path
            );
        }
    }

    /**
     * Sanitizes a user-controlled string parameter by escaping HTML characters to prevent injection attacks.
     *
     * @param input the string to sanitize
     * @return the sanitized string, or null if input was null
     */
    public String sanitizeInputString(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
