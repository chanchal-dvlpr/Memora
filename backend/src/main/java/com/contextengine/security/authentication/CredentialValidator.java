package com.contextengine.security.authentication;

import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import java.util.List;
import java.util.Objects;

/**
 * Component responsible for validating credentials and security tokens against registered configuration values.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Credential Validation)
 * Responsibility: Verify that incoming credentials/tokens match authorized keys, throwing security exceptions on mismatch.
 * Dependencies: {@link SecurityConstants}, {@link SecurityException}
 * Future Usage: Utilized by AuthenticationService to perform low-level signature and key validations.
 * </p>
 */
public class CredentialValidator {

    private final List<String> authorizedTokens;

    /**
     * Constructs a CredentialValidator.
     *
     * @param authorizedTokens the list of valid security tokens loaded from application properties
     */
    public CredentialValidator(List<String> authorizedTokens) {
        this.authorizedTokens = authorizedTokens != null ? List.copyOf(authorizedTokens) : List.of();
    }

    /**
     * Asserts that a token is valid.
     * If no tokens are configured, the authentication checks pass (open/default mode).
     *
     * @param token the security token to validate
     * @throws SecurityException if the token is invalid or missing when keys are configured
     */
    public void validateToken(String token) {
        if (authorizedTokens.isEmpty()) {
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            throw new SecurityException(
                SecurityConstants.ERROR_AUTHENTICATION_FAILED,
                "Authentication failed: Security token is missing or empty"
            );
        }

        if (!authorizedTokens.contains(token.trim())) {
            throw new SecurityException(
                SecurityConstants.ERROR_AUTHENTICATION_FAILED,
                "Authentication failed: Invalid security token provided"
            );
        }
    }
}
