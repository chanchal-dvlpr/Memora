package com.contextengine.mcp.session;

import com.contextengine.mcp.McpContext;
import com.contextengine.mcp.McpSessionState;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the active registries, transitions, and timeouts of Model Context Protocol (MCP) sessions.
 *
 * <p>Architecture Reference: SRS-CE-FR-020 (Session Lifecycle Model) / SRS-CE-CH4-4.4 (Session Sync)
 * Responsibility: Track connection states sequentially, manage transitions, and execute cleanup sweeps of idle sessions.
 * Dependencies: {@link McpSession}, {@link McpContext}, {@link McpSessionState}
 * Future Usage: Bound to loopback network transport listeners or web socket hooks to manage client connection lifecycles.
 */
public class McpSessionManager {

    private static final long SESSION_TIMEOUT_SECONDS = 300L; // 300 seconds inactive threshold

    private final Map<String, McpSession> sessionStore;

    /**
     * Constructs the McpSessionManager.
     */
    public McpSessionManager() {
        this.sessionStore = new ConcurrentHashMap<>();
    }

    /**
     * Initializes and registers a new MCP session.
     * Starts lifecycle transition sequence: DISCOVERED -> CONNECTING.
     *
     * @param clientName the client identifier
     * @param clientVersion the client version
     * @return the created session
     */
    public McpSession createSession(String clientName, String clientVersion) {
        McpContext context = new McpContext(clientName, clientVersion);
        McpSession session = new McpSession(context);

        // Record initial transition from DISCOVERED to CONNECTING
        session.transitionTo(McpSessionState.CONNECTING);
        sessionStore.put(session.getSessionId(), session);

        return session;
    }

    /**
     * Retrieves an active session by identifier and touches it.
     *
     * @param sessionId the session identifier
     * @return an optional containing the session, or empty if not found
     */
    public Optional<McpSession> getSession(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        McpSession session = sessionStore.get(sessionId);
        if (session != null) {
            session.touch();
            return Optional.of(session);
        }
        return Optional.empty();
    }

    /**
     * Transitions an active session to CONNECTED state.
     * Sequence: CONNECTING -> HANDSHAKING -> AUTHENTICATED -> AUTHORIZED -> CONNECTED.
     *
     * @param sessionId the session identifier
     */
    public void establishSession(String sessionId) {
        getSession(sessionId).ifPresent(session -> {
            session.transitionTo(McpSessionState.HANDSHAKING);
            session.transitionTo(McpSessionState.AUTHENTICATED);
            session.getContext().grantPermission("mcp:read");
            session.getContext().grantPermission("mcp:tool_invoke");
            session.transitionTo(McpSessionState.AUTHORIZED);
            session.transitionTo(McpSessionState.CONNECTED);
            session.transitionTo(McpSessionState.ACTIVE);
        });
    }

    /**
     * Terminates and removes a session from active store registry.
     * Sequence: ACTIVE -> DISCONNECTED -> TERMINATED.
     *
     * @param sessionId the session identifier
     */
    public void terminateSession(String sessionId) {
        McpSession session = sessionStore.get(sessionId);
        if (session != null) {
            session.transitionTo(McpSessionState.DISCONNECTED);
            session.transitionTo(McpSessionState.TERMINATED);
            sessionStore.remove(sessionId);
        }
    }

    /**
     * Performs a sweep and prunes all inactive session registrations.
     */
    public void pruneExpiredSessions() {
        sessionStore.values().removeIf(session -> {
            boolean expired = session.isExpired(SESSION_TIMEOUT_SECONDS);
            if (expired) {
                // Perform clean transition before removal
                session.transitionTo(McpSessionState.DISCONNECTED);
                session.transitionTo(McpSessionState.TERMINATED);
            }
            return expired;
        });
    }

    /**
     * Retrieves a read-only list of all active sessions.
     *
     * @return collection of sessions
     */
    public Collection<McpSession> getActiveSessions() {
        return Collections.unmodifiableCollection(sessionStore.values());
    }
}
