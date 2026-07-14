package com.contextengine.mcp.session;

import com.contextengine.mcp.McpContext;
import com.contextengine.mcp.McpSessionState;
import java.util.Objects;

/**
 * Encapsulates an active, stateful Model Context Protocol connection session.
 *
 * <p>Architecture Reference: SRS-CE-FR-020 (Session Lifecycle Model)
 * Responsibility: Track session timestamps (creation and last-activity) and wrap the underlying McpContext.
 * Dependencies: {@link McpContext}, {@link McpSessionState}
 * Future Usage: Queried by the session registry to check for idle timeouts and session state validation.
 */
public class McpSession {

    private final McpContext context;
    private final long createdTimestamp;
    private long lastAccessedTimestamp;

    /**
     * Constructs a stateful McpSession wrapping a context.
     *
     * @param context the underlying McpContext
     */
    public McpSession(McpContext context) {
        this.context = Objects.requireNonNull(context, "McpContext must not be null");
        this.createdTimestamp = System.currentTimeMillis();
        this.lastAccessedTimestamp = this.createdTimestamp;
    }

    public McpContext getContext() {
        return context;
    }

    public String getSessionId() {
        return context.getSessionId();
    }

    public McpSessionState getState() {
        return context.getState();
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public long getLastAccessedTimestamp() {
        return lastAccessedTimestamp;
    }

    /**
     * Refreshes the last active timestamp of the session.
     */
    public void touch() {
        this.lastAccessedTimestamp = System.currentTimeMillis();
    }

    /**
     * Transitions the session state and updates activity timestamp.
     *
     * @param newState the target state
     */
    public void transitionTo(McpSessionState newState) {
        context.transitionTo(newState);
        touch();
    }

    /**
     * Checks if the session is inactive beyond a certain threshold duration.
     *
     * @param thresholdSeconds duration in seconds
     * @return true if expired, false otherwise
     */
    public boolean isExpired(long thresholdSeconds) {
        long current = System.currentTimeMillis();
        return (current - lastAccessedTimestamp) > (thresholdSeconds * 1000L);
    }
}
