package com.contextengine.mcp;

/**
 * Common constants for the Model Context Protocol (MCP) subsystem.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 (Section 4.4 - Error Interface & Protocol Code Matrix)
 * Responsibility: Provide a central, type-safe registry of protocol version tokens, tool identifiers,
 * and JSON-RPC error codes.
 * Dependencies: None.
 * Future Usage: Used by protocol handlers to validate incoming request envelopes, route tool calls,
 * and construct standard error response payloads.
 */
public final class McpConstants {

    private McpConstants() {
        // Prevent instantiation
    }

    /**
     * The supported JSON-RPC protocol version.
     */
    public static final String JSONRPC_VERSION = "2.0";

    // Tool Names
    public static final String TOOL_REGISTER_PROJECT = "context_engine_register_project";
    public static final String TOOL_SCAN_PROJECT = "context_engine_scan_project";
    public static final String TOOL_GENERATE_CONTEXT = "generate_context";
    public static final String TOOL_GENERATE_CONTEXT_SNAPSHOT = "generate_context_snapshot";
    public static final String TOOL_CORE_SEARCH = "core_search";
    public static final String TOOL_SEARCH_ENGINEERING_MEMORY = "search_engineering_memory";

    // JSON-RPC Standard Error Codes
    public static final int ERROR_PARSE_ERROR = -32700;
    public static final int ERROR_INVALID_REQUEST = -32600;
    public static final int ERROR_METHOD_NOT_FOUND = -32601;
    public static final int ERROR_INVALID_PARAMS = -32602;
    public static final int ERROR_INTERNAL_ERROR = -32603;

    // Platform Specific/MCP Specific Error Codes
    public static final int ERROR_WORKSPACE_LOCKED = -32001;
    public static final int ERROR_TOKEN_CAP_BREACH = -32002;
    public static final int ERROR_AUTHENTICATION_FAILED = 4001;
    public static final int ERROR_SCOPE_INSUFFICIENT = 4002;
    public static final int ERROR_GROUNDING_VIOLATION = 4003;
    public static final int ERROR_CONSTRAINT_BREACH = 4004;
}
