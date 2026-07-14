package com.contextengine.mcp.tool;

import com.contextengine.application.command.ScanProjectCommand;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.McpException;
import com.contextengine.mcp.protocol.McpRequest;
import java.util.Map;
import java.util.Objects;

/**
 * MCP tool for triggering active codebase workspace AST and metadata scans.
 *
 * <p>Architecture Reference: srs_project_scanner_fr002.pdf (Section 21 - MCP Server Requirements)
 * Responsibility: Translate raw parameters into ScanProjectCommand, execute scans, and return progress/success states.
 * Dependencies: ProjectApplicationService, ScanProjectCommand, ProjectId, McpConstants, McpException
 * Future Usage: Exposes scanning invocation capabilities to LLM agents calling context_engine_scan_project.
 */
public class ScanProjectTool implements McpTool {

    private final ProjectApplicationService projectApplicationService;

    /**
     * Constructs the ScanProjectTool.
     *
     * @param projectApplicationService the backing application service
     */
    public ScanProjectTool(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = Objects.requireNonNull(projectApplicationService, "ProjectApplicationService must not be null");
    }

    @Override
    public String getName() {
        return McpConstants.TOOL_SCAN_PROJECT;
    }

    @Override
    public Object execute(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("projectId")) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameter: projectId");
        }

        String projectIdStr = (String) params.get("projectId");
        if (projectIdStr == null || projectIdStr.trim().isEmpty()) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "projectId parameter must not be empty");
        }

        String scanModality = (String) params.get("scanModality");
        boolean deep = "FULL".equalsIgnoreCase(scanModality);

        try {
            ProjectId projectId = ProjectId.fromString(projectIdStr);
            ScanProjectCommand command = new ScanProjectCommand(projectId, deep, true);

            ApplicationResult<Boolean> result = projectApplicationService.scanProject(command);

            if (result.isFailure()) {
                return Map.of(
                    "isError", true,
                    "errorCode", "SCAN_FAILED",
                    "message", result.error().isPresent() && result.error().get().getMessage() != null ? result.error().get().getMessage() : "Unknown scanning failure"
                );
            }

            return Map.of(
                "isError", false,
                "success", result.value().orElse(false)
            );

        } catch (IllegalArgumentException e) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Invalid projectId format: " + e.getMessage());
        }
    }
}
