package com.contextengine.mcp;

/**
 * Enumerates the standard lifecycle states of a Model Context Protocol (MCP) session.
 *
 * <p>Architecture Reference: SRS-CE-FR-020 (Session Lifecycle Model)
 * Responsibility: Provide a strongly-typed enum mapping the state progression of MCP client interactions.
 * Dependencies: None.
 * Future Usage: State machine checks when performing session setup, capability verification, or offline sync.
 */
public enum McpSessionState {
    DISCOVERED,
    CONNECTING,
    HANDSHAKING,
    AUTHENTICATED,
    AUTHORIZED,
    CONNECTED,
    ACTIVE,
    SYNCHRONIZING,
    IDLE,
    DISCONNECTED,
    TERMINATED
}
