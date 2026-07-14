package com.contextengine.security.foundation;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Thread-local context tracking the current authenticated client identity and associated authorization scopes.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Shared Security Context)
 * Responsibility: Maintain request-scoped client principals and check capability scopes in a thread-safe manner.
 * Dependencies: {@link Principal}
 * Future Usage: Queried by interceptors, authorization guards, and audit loggers during request handling.
 * </p>
 */
public final class SecurityContext {

    private static final ThreadLocal<Principal> CONTEXT_HOLDER = new ThreadLocal<>();

    private SecurityContext() {
        // Prevent instantiation
    }

    /**
     * Binds the authenticated principal context to the current execution thread.
     *
     * @param principal the authenticated identity context
     */
    public static void setPrincipal(Principal principal) {
        CONTEXT_HOLDER.set(principal);
    }

    /**
     * Resolves the currently active principal bound to this execution thread.
     *
     * @return optional containing the active Principal, or empty if unauthenticated
     */
    public static Optional<Principal> getPrincipal() {
        return Optional.ofNullable(CONTEXT_HOLDER.get());
    }

    /**
     * Checks if the active principal context contains the required security scope.
     *
     * @param requiredScope the scope permission string to check
     * @return true if authenticated and scope is held, false otherwise
     */
    public static boolean hasScope(String requiredScope) {
        Objects.requireNonNull(requiredScope, "Required scope must not be null");
        Principal principal = CONTEXT_HOLDER.get();
        return principal != null && principal.scopes().contains(requiredScope);
    }

    /**
     * Clears the context bound to the current thread to prevent memory/security leaks in pooled thread models.
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * Record representing the authenticated user principal.
     */
    public record Principal(String identity, Set<String> scopes) {

        /**
         * Constructs a Principal record.
         *
         * @param identity the client tool name or session identifier
         * @param scopes the set of scopes granted to this client
         */
        public Principal {
            Objects.requireNonNull(identity, "Identity must not be null");
            scopes = scopes != null ? Set.copyOf(scopes) : Collections.emptySet();
        }
    }
}
