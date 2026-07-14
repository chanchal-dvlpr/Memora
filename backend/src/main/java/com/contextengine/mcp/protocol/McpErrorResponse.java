package com.contextengine.mcp.protocol;

/**
 * Model representing an error Model Context Protocol (MCP) JSON-RPC response envelope.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 (Section 4.4 - Error Interface & Protocol Code Matrix)
 * Responsibility: Capture failure responses, providing standard error codes and diagnostic payloads.
 * Dependencies: McpError
 * Future Usage: Serialized and written back to client streams after parser or execution failures.
 */
public class McpErrorResponse {

    private String jsonrpc;
    private McpError error;
    private Object id;

    /**
     * Default constructor for JSON deserializers.
     */
    public McpErrorResponse() {
    }

    /**
     * Constructs an McpErrorResponse.
     *
     * @param jsonrpc the JSON-RPC version
     * @param error the structured error details
     * @param id the correlated request identifier
     */
    public McpErrorResponse(String jsonrpc, McpError error, Object id) {
        this.jsonrpc = jsonrpc;
        this.error = error;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public McpError getError() {
        return error;
    }

    public void setError(McpError error) {
        this.error = error;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    /**
     * Embedded JSON-RPC error payload record.
     *
     * @param code the numeric error code mapping to McpConstants
     * @param message a short description of the error condition
     * @param data optional metadata or validation trace map
     */
    public record McpError(int code, String message, Object data) {
    }
}
