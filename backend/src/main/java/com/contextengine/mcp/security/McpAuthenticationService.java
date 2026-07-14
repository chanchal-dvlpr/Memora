package com.contextengine.mcp.security;

import com.contextengine.mcp.McpSessionState;
import com.contextengine.mcp.session.McpSession;
import com.contextengine.mcp.session.McpSessionManager;
import java.util.List;
import java.util.Objects;

/**
 * Service executing identity verification on incoming MCP setup handshakes.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Authentication)
 * Responsibility: Verify client security tokens against registered workspace access keys and initialize active sessions.
 * Dependencies: {@link McpSessionManager}, {@link McpSecurityException}
 * Future Usage: Bound to loopback interface endpoints to authenticate cursor/agent clients.
 */
public class McpAuthenticationService {

    private final List<String> authorizedTokens;
    private final McpSessionManager sessionManager;

    /**
     * Constructs a McpAuthenticationService.
     *
     * @param authorizedTokens list of authorized tokens from configuration properties
     * @param sessionManager session manager for establishing connections
     */
    public McpAuthenticationService(List<String> authorizedTokens, McpSessionManager sessionManager) {
        this.authorizedTokens = authorizedTokens != null ? List.copyOf(authorizedTokens) : List.of();
        this.sessionManager = Objects.requireNonNull(sessionManager, "McpSessionManager must not be null");
    }

    /**
     * Authenticates an client connection attempt using a token.
     * If the token list is configured, checks presence of a valid matching key.
     *
     * @param token the security token submitted by client
     * @param clientName name of client tool
     * @param clientVersion version of client tool
     * @return the authenticated McpSession
     * @throws McpSecurityException if token validation fails (4001)
     */
    public McpSession authenticate(String token, String clientName, String clientVersion) {
        if (!authorizedTokens.isEmpty()) {
            if (token == null || token.trim().isEmpty() || !authorizedTokens.contains(token.trim())) {
                throw new McpSecurityException(4001, "Authentication failed: Missing or invalid security token");
            }
        }

        McpSession session = sessionManager.createSession(clientName, clientVersion);
        session.transitionTo(McpSessionState.AUTHENTICATED);

        // Grant scopes dynamically based on authentication
        session.getContext().grantPermission("mcp:read");
        session.getContext().grantPermission("mcp:tool_invoke");
        if (authorizedTokens.isEmpty() || (token != null && authorizedTokens.contains(token.trim()))) {
            session.getContext().grantPermission("mcp:write_graph");
        }

        return session;
    }

    /**
     * Checks if security tokens are configured, meaning client authentication is required.
     *
     * @return true if authentication is required, false otherwise
     */
    public boolean isAuthenticationRequired() {
        return !authorizedTokens.isEmpty();
    }
}
