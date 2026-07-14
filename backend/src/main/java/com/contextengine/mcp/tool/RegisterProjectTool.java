package com.contextengine.mcp.tool;

import com.contextengine.application.command.RegisterProjectCommand;
import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.McpException;
import com.contextengine.mcp.protocol.McpRequest;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP tool for registering a project workspace codebase directory.
 *
 * <p>Architecture Reference: srs_project_registration_fr001.pdf (Section 18 - MCP Server Requirements)
 * Responsibility: Translate raw tool parameters into RegisterProjectCommand and return project metadata or error payloads.
 * Dependencies: ProjectApplicationService, RegisterProjectCommand, Path, McpConstants, McpException
 * Future Usage: Exposes register capability directly to LLM agents calling context_engine_register_project.
 */
public class RegisterProjectTool implements McpTool {

    private final ProjectApplicationService projectApplicationService;

    /**
     * Constructs the RegisterProjectTool.
     *
     * @param projectApplicationService the backing application service
     */
    public RegisterProjectTool(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = Objects.requireNonNull(projectApplicationService, "ProjectApplicationService must not be null");
    }

    @Override
    public String getName() {
        return McpConstants.TOOL_REGISTER_PROJECT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("targetPath")) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameter: targetPath");
        }

        String targetPathStr = (String) params.get("targetPath");
        if (targetPathStr == null || targetPathStr.trim().isEmpty()) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "targetPath parameter must not be empty");
        }

        String customName = (String) params.get("customName");
        List<String> exclusionRules = (List<String>) params.get("exclusionRules");

        try {
            // Resolve relative paths to absolute using standard host system context
            File resolvedFile = new File(targetPathStr).getAbsoluteFile();
            String absolutePath = resolvedFile.getCanonicalPath();

            // Create Path value object (checks traversal boundaries)
            Path path = new Path(absolutePath);

            String title = (customName != null && !customName.trim().isEmpty()) ? customName.trim() : resolvedFile.getName();
            if (title.isEmpty()) {
                title = "Unnamed Project";
            }

            RegisterProjectCommand command = new RegisterProjectCommand(path, title, exclusionRules);
            ApplicationResult<ProjectDto> result = projectApplicationService.registerProject(command);

            if (result.isFailure()) {
                return Map.of(
                    "isError", true,
                    "errorCode", "REGISTRATION_FAILED",
                    "message", result.error().isPresent() && result.error().get().getMessage() != null ? result.error().get().getMessage() : "Unknown registration failure"
                );
            }

            ProjectDto dto = result.value().orElseThrow();
            return Map.of(
                "isError", false,
                "projectId", dto.id(),
                "canonicalPath", dto.absoluteRootPath(),
                "projectName", dto.title()
            );

        } catch (IllegalArgumentException e) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return Map.of(
                "isError", true,
                "errorCode", "RESOLVE_PATH_FAILED",
                "message", e.getMessage() != null ? e.getMessage() : "Could not canonicalize directory path"
            );
        }
    }
}
