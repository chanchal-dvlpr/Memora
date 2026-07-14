package com.contextengine.security.authentication;

import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityContext;
import java.util.Objects;
import java.util.Set;

/**
 * Service managing client identity verification and binding authenticated contexts to the execution thread.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Authentication Service)
 * Responsibility: Enforce credential checks, resolve granted scopes, and establish active SecurityContext sessions.
 * Dependencies: {@link CredentialValidator}, {@link SecurityContext}, {@link SecurityConstants}
 * Future Usage: Queried on rest controllers or standard stream protocols to authenticate cursors/agents.
 * </p>
 */
public class AuthenticationService {

    private final CredentialValidator credentialValidator;

    /**
     * Constructs an AuthenticationService.
     *
     * @param credentialValidator validator to confirm token credentials
     */
    public AuthenticationService(CredentialValidator credentialValidator) {
        this.credentialValidator = Objects.requireNonNull(credentialValidator, "CredentialValidator must not be null");
    }

    /**
     * Authenticates an incoming client request and establishes the security principal context.
     *
     * @param token the security token credential
     * @param clientIdentity client name or application identifier
     */
    public void authenticate(String token, String clientIdentity) {
        String finalIdentity = (clientIdentity == null || clientIdentity.trim().isEmpty()) ? "unknown-client" : clientIdentity.trim();

        // Perform token checks
        credentialValidator.validateToken(token);

        // Grant scopes dynamically based on authentication success
        Set<String> scopes = Set.of(
            SecurityConstants.SCOPE_READ,
            SecurityConstants.SCOPE_TOOL_INVOKE,
            SecurityConstants.SCOPE_WRITE_GRAPH
        );

        // Bind the principal context to the ThreadLocal holder
        SecurityContext.setPrincipal(new SecurityContext.Principal(finalIdentity, scopes));
    }

    /**
     * Clears the current active thread-local security context.
     */
    public void clear() {
        SecurityContext.clear();
    }
}
