package com.contextengine.security.authorization;

import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityContext;
import com.contextengine.security.foundation.SecurityException;
import java.util.Objects;

/**
 * Service enforcing scope-based permission checks at the operation or application service level.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Authorization Service)
 * Responsibility: Inspect the thread-local SecurityContext, asserting that the active principal holds the necessary scope permission.
 * Dependencies: {@link SecurityContext}, {@link SecurityConstants}, {@link SecurityException}
 * Future Usage: Bound to use case execution gates and command dispatches to prevent privilege escalation.
 * </p>
 */
public class AuthorizationService {

    /**
     * Asserts that the currently active principal holds the specified scope permission.
     *
     * @param requiredScope the permission scope token required (e.g. {@code mcp:read})
     * @throws SecurityException if the principal is unauthenticated (4001) or lacks the required scope (4002)
     */
    public void checkPermission(String requiredScope) {
        Objects.requireNonNull(requiredScope, "Required scope must not be null");

        SecurityContext.Principal principal = SecurityContext.getPrincipal().orElse(null);
        if (principal == null) {
            throw new SecurityException(
                SecurityConstants.ERROR_AUTHENTICATION_FAILED,
                "Authorization failed: Request is unauthenticated"
            );
        }

        if (!principal.scopes().contains(requiredScope)) {
            throw new SecurityException(
                SecurityConstants.ERROR_SCOPE_INSUFFICIENT,
                "Authorization failed: Principal lacks required scope: " + requiredScope
            );
        }
    }
}
