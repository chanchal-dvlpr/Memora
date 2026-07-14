package com.contextengine.mcp.transport;

/**
 * Interface representing a physical connection transport for MCP.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Local Transport Isolation)
 * Responsibility: Abstract transport starting and stopping procedures, keeping protocol handler decoupled from streams/sockets.
 * Dependencies: None.
 * Future Usage: Controlled by Spring lifecycle beans to start stdio loops or network listeners.
 */
public interface McpTransport {

    /**
     * Activates the physical transport channel listeners.
     */
    void start();

    /**
     * Gracefully tears down connection channels and stops listeners.
     */
    void stop();
}
