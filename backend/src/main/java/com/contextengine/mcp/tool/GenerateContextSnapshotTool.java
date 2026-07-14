package com.contextengine.mcp.tool;

import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.protocol.McpRequest;

/**
 * MCP tool for compiling dynamic, token-bounded codebase context payloads under the generate_context_snapshot namespace.
 *
 * <p>Architecture Reference: srs_context_generation_fr014.pdf / SRS-CE-CH4-4.4
 * Responsibility: Route generate_context_snapshot tool executions to the underlying GenerateContextTool logic.
 * Dependencies: ContextApplicationService, GenerateContextTool, McpConstants, McpRequest
 * Future Usage: Exposes context snapshot compilation directly to external LLM clients calling generate_context_snapshot.
 */
public class GenerateContextSnapshotTool extends GenerateContextTool {

    /**
     * Constructs the GenerateContextSnapshotTool.
     *
     * @param contextApplicationService the context application service
     */
    public GenerateContextSnapshotTool(ContextApplicationService contextApplicationService) {
        super(contextApplicationService);
    }

    @Override
    public String getName() {
        return McpConstants.TOOL_GENERATE_CONTEXT_SNAPSHOT;
    }
}
