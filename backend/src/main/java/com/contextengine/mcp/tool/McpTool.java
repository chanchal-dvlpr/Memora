package com.contextengine.mcp.tool;

import com.contextengine.mcp.protocol.McpRequest;

/**
 * Interface defining the execution and metadata contract for Model Context Protocol (MCP) tools.
 *
 * <p>Architecture Reference: SRS-CE-FR-020 (MCP Tool Model)
 * Responsibility: Provide the unified abstraction to map incoming tool requests into core application commands.
 * Dependencies: McpRequest
 * Future Usage: Implemented by tool executors like RegisterProjectTool, ScanProjectTool, and SearchTool.
 */
public interface McpTool {

    /**
     * Retrieves the unique tool name registered on the server (e.g. context_engine_register_project).
     *
     * @return the unique tool name
     */
    String getName();

    /**
     * Executes the tool use-case mapping parameters, invoking the underlying application services.
     *
     * @param request the inbound JSON-RPC request containing the parameters map
     * @return the execution result payload to wrap in the JSON-RPC response result
     * @throws com.contextengine.mcp.McpException if parameter validation, authorization, or execution fails
     */
    Object execute(McpRequest request);
}
