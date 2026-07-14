package com.contextengine.mcp.tool;

import com.contextengine.application.command.GenerateContextCommand;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.domain.service.FormatEnum;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SearchQuery;
import com.contextengine.domain.valueobject.TokenBudget;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.McpException;
import com.contextengine.mcp.protocol.McpRequest;
import java.util.Map;
import java.util.Objects;

/**
 * MCP tool for compiling dynamic, token-bounded codebase context payloads.
 *
 * <p>Architecture Reference: srs_context_generation_fr014.pdf (Section 11/12 - Validation & Business Invariants)
 * Responsibility: Map request params to GenerateContextCommand, invoke the Context application service, and return standard result envelopes.
 * Dependencies: ContextApplicationService, GenerateContextCommand, ProjectId, SearchQuery, NodeId, TokenBudget, FormatEnum, McpConstants, McpException
 * Future Usage: Exposes context synthesis directly to external LLM clients calling generate_context or generate_context_snapshot.
 */
public class GenerateContextTool implements McpTool {

    private final ContextApplicationService contextApplicationService;

    /**
     * Constructs the GenerateContextTool.
     *
     * @param contextApplicationService the context application service
     */
    public GenerateContextTool(ContextApplicationService contextApplicationService) {
        this.contextApplicationService = Objects.requireNonNull(contextApplicationService, "ContextApplicationService must not be null");
    }

    @Override
    public String getName() {
        return McpConstants.TOOL_GENERATE_CONTEXT;
    }

    @Override
    public Object execute(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("projectId") || !params.containsKey("contextQuery") || !params.containsKey("tokenBudget")) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameters: projectId, contextQuery, and tokenBudget must be supplied");
        }

        String projectIdStr = (String) params.get("projectId");
        String contextQueryStr = (String) params.get("contextQuery");
        Object tokenBudgetObj = params.get("tokenBudget");

        if (projectIdStr == null || projectIdStr.trim().isEmpty() || contextQueryStr == null || contextQueryStr.trim().isEmpty() || tokenBudgetObj == null) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Required parameters must have non-empty values");
        }

        int budgetVal;
        try {
            if (tokenBudgetObj instanceof Number number) {
                budgetVal = number.intValue();
            } else {
                budgetVal = Integer.parseInt(tokenBudgetObj.toString());
            }
        } catch (Exception e) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Invalid tokenBudget value: " + tokenBudgetObj);
        }

        String focusNodeStr = (String) params.get("focusNode");
        String formatStr = (String) params.get("format");

        try {
            ProjectId projectId = ProjectId.fromString(projectIdStr);
            SearchQuery query = new SearchQuery(contextQueryStr, false, Metadata.empty(), 50);
            NodeId focusNode = (focusNodeStr != null && !focusNodeStr.trim().isEmpty()) ? NodeId.fromString(focusNodeStr) : NodeId.generate();
            TokenBudget budget = new TokenBudget(budgetVal);

            FormatEnum format = FormatEnum.MARKDOWN;
            if (formatStr != null) {
                try {
                    format = FormatEnum.valueOf(formatStr.toUpperCase().trim());
                } catch (IllegalArgumentException ignored) {
                }
            }

            GenerateContextCommand command = new GenerateContextCommand(projectId, query, focusNode, budget, format);
            ApplicationResult<ContextSnapshotDto> result = contextApplicationService.generateContext(command);

            if (result.isFailure()) {
                int code = McpConstants.ERROR_INTERNAL_ERROR;
                String msg = result.error().isPresent() && result.error().get().getMessage() != null ? result.error().get().getMessage() : "Unknown failure compiling context snapshot";
                // Map validation exceptions
                if (msg != null && msg.contains("Validation failed")) {
                    code = McpConstants.ERROR_INVALID_PARAMS;
                }
                return Map.of(
                    "isError", true,
                    "errorCode", "CONTEXT_GENERATION_FAILED",
                    "message", msg != null ? msg : "Unknown failure compiling context snapshot"
                );
            }

            ContextSnapshotDto dto = result.value().orElseThrow();
            return Map.of(
                "isError", false,
                "contextSessionId", dto.id(),
                "targetViewProfile", format.toString(),
                "tokensConsumed", dto.tokensUsed(),
                "contextPayload", dto.payload(),
                "provenanceMap", Map.of()
            );

        } catch (IllegalArgumentException e) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, e.getMessage());
        }
    }
}
