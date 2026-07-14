package com.contextengine.mcp.protocol;

/**
 * Model representing a successful Model Context Protocol (MCP) JSON-RPC response envelope.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 (Section 4.4 - Protocol Compatibility & Session Sync)
 * Responsibility: Capture success result payloads alongside the correlation identifier.
 * Dependencies: None.
 * Future Usage: Serialized and written back to client streams after successful tool executions.
 */
public class McpResponse {

    private String jsonrpc;
    private Object result;
    private Object id;

    /**
     * Default constructor for JSON deserializers.
     */
    public McpResponse() {
    }

    /**
     * Constructs an McpResponse.
     *
     * @param jsonrpc the JSON-RPC version
     * @param result the returned result payload
     * @param id the correlated request identifier
     */
    public McpResponse(String jsonrpc, Object result, Object id) {
        this.jsonrpc = jsonrpc;
        this.result = result;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }
}
