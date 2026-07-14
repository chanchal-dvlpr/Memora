package com.contextengine.mcp.tool;

import com.contextengine.application.dto.KnowledgeNodeDto;
import com.contextengine.application.query.SearchProjectQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.usecase.SearchUseCase;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SearchQuery;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.McpException;
import com.contextengine.mcp.protocol.McpRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MCP tool for executing natural-language and hybrid keyword query lookups over the engineering memory.
 *
 * <p>Architecture Reference: srs_search_query_fr015.pdf / SRS-CE-CH4-4.4
 * Responsibility: Translate MCP params to SearchProjectQuery, delegate to SearchUseCase, and return mapped results.
 * Dependencies: {@link SearchUseCase}, {@link SearchProjectQuery}, {@link McpConstants}, {@link McpException}
 * Future Usage: Exposes search capabilities directly to LLM agents calling search_engineering_memory or core_search.
 */
public class SearchTool implements McpTool {

    protected final SearchUseCase searchUseCase;

    /**
     * Constructs the SearchTool.
     *
     * @param searchUseCase the search use case abstraction
     */
    public SearchTool(SearchUseCase searchUseCase) {
        this.searchUseCase = Objects.requireNonNull(searchUseCase, "SearchUseCase must not be null");
    }

    @Override
    public String getName() {
        return McpConstants.TOOL_SEARCH_ENGINEERING_MEMORY;
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

        // Accept query_string (from external interface spec) or term as query focus parameter
        String queryText = (String) params.get("query_string");
        if (queryText == null) {
            queryText = (String) params.get("term");
        }

        if (queryText == null || queryText.trim().isEmpty()) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing query search text parameter (query_string or term)");
        }

        try {
            ProjectId projectId = ProjectId.fromString(projectIdStr);
            SearchQuery searchQuery = new SearchQuery(queryText, false, Metadata.empty(), 50);
            SearchProjectQuery query = new SearchProjectQuery(projectId, searchQuery);

            ApplicationResult<List<KnowledgeNodeDto>> result = searchUseCase.execute(query);

            if (result.isFailure()) {
                return Map.of(
                    "isError", true,
                    "errorCode", "SEARCH_FAILED",
                    "message", result.error().isPresent() && result.error().get().getMessage() != null 
                        ? result.error().get().getMessage() : "Unknown search failure"
                );
            }

            List<Map<String, Object>> results = result.value().orElse(List.of()).stream()
                .map(dto -> Map.<String, Object>of(
                    "id", dto.id(),
                    "type", dto.type(),
                    "attributes", dto.attributes()
                ))
                .collect(Collectors.toList());

            return Map.of(
                "isError", false,
                "results", results
            );

        } catch (IllegalArgumentException e) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, e.getMessage());
        }
    }
}
