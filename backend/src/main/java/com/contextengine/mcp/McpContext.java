package com.contextengine.mcp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Model Context Protocol (MCP) session-specific contexts, capabilities, and lifecycle states.
 *
 * <p>Architecture Reference: SRS-CE-FR-020 (Session Lifecycle Model & Scope Matrix)
 * Responsibility: Track the active client identities, negotiated capabilities, and session authorization scopes.
 * Dependencies: McpSessionState
 * Future Usage: Utilized by request handlers and validation engines to enforce security boundaries and track session durations.
 */
public class McpContext {

    private final String sessionId;
    private final String clientName;
    private final String clientVersion;
    private final Set<String> permissions;
    private McpSessionState state;

    /**
     * Constructs a new McpContext with client descriptors.
     *
     * @param clientName the name of the client application
     * @param clientVersion the version string of the client
     */
    public McpContext(String clientName, String clientVersion) {
        this.sessionId = UUID.randomUUID().toString();
        this.clientName = Objects.requireNonNull(clientName, "Client name must not be null");
        this.clientVersion = Objects.requireNonNull(clientVersion, "Client version must not be null");
        this.state = McpSessionState.DISCOVERED;
        this.permissions = new HashSet<>();
    }

    /**
     * Retrieves the session identifier.
     *
     * @return the unique session identifier
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Retrieves the client name.
     *
     * @return the client name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Retrieves the client version.
     *
     * @return the client version
     */
    public String getClientVersion() {
        return clientVersion;
    }

    /**
     * Retrieves the current session lifecycle state.
     *
     * @return the session state
     */
    public McpSessionState getState() {
        return state;
    }

    /**
     * Transitions the session state.
     *
     * @param newState the target session state
     */
    public void transitionTo(McpSessionState newState) {
        this.state = Objects.requireNonNull(newState, "New state must not be null");
    }

    /**
     * Adds an authorization permission scope token.
     *
     * @param permission the permission token (e.g. mcp:read)
     */
    public void grantPermission(String permission) {
        if (permission != null) {
            this.permissions.add(permission);
        }
    }

    /**
     * Checks if the active session holds a specific permission scope.
     *
     * @param permission the permission token to verify
     * @return true if authorized, false otherwise
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * Retrieves the current granted permissions.
     *
     * @return immutable set of permissions
     */
    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }
}
