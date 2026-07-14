package com.contextengine.mcp.security;

import com.contextengine.mcp.session.McpSession;
import java.util.Objects;

/**
 * Service that enforces authorization policies on authenticated MCP sessions.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Permissions)
 * Responsibility: Ensure active sessions hold sufficient privileges (mcp:read, mcp:tool_invoke, mcp:write_graph) before processing actions.
 * Dependencies: {@link McpSession}, {@link McpSecurityException}
 * Future Usage: Queried by the protocol handler or transport layer interceptor before tool invocation dispatch.
 */
public class McpAuthorizationService {

    /**
     * Verifies that the session holds the required scope token.
     *
     * @param session the active client session
     * @param requiredScope the permission token required (e.g. mcp:read)
     * @throws McpSecurityException if privileges are insufficient (4002)
     */
    public void authorize(McpSession session, String requiredScope) {
        Objects.requireNonNull(requiredScope, "Required scope must not be null");

        if (session == null) {
            throw new McpSecurityException(4001, "Authentication failed: Session is null or expired");
        }

        if (!session.getContext().hasPermission(requiredScope)) {
            throw new McpSecurityException(4002, "Scope insufficient: Session lacks required scope: " + requiredScope);
        }
    }
}
