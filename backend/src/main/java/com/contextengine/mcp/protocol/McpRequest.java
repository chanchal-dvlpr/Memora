package com.contextengine.mcp.protocol;

import java.util.Collections;
import java.util.Map;

/**
 * Model representing an inbound Model Context Protocol (MCP) JSON-RPC request envelope.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 (Section 4.4 - Protocol Compatibility & Session Sync)
 * Responsibility: Capture request parameters (jsonrpc, method, params, id) in a standard serializable format.
 * Dependencies: None.
 * Future Usage: Parsed by incoming request listeners and routed to tools by McpProtocolHandler.
 */
public class McpRequest {

    private String jsonrpc;
    private String method;
    private Map<String, Object> params;
    private Object id;

    /**
     * Default constructor for JSON deserializers.
     */
    public McpRequest() {
        this.params = Collections.emptyMap();
    }

    /**
     * Constructs an McpRequest.
     *
     * @param jsonrpc the JSON-RPC version
     * @param method the method/tool name to execute
     * @param params parameter mapping for the execution context
     * @param id the request sequence identifier
     */
    public McpRequest(String jsonrpc, String method, Map<String, Object> params, Object id) {
        this.jsonrpc = jsonrpc;
        this.method = method;
        this.params = params != null ? Map.copyOf(params) : Collections.emptyMap();
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params != null ? Map.copyOf(params) : Collections.emptyMap();
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }
}
