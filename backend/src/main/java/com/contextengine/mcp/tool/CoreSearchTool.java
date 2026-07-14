package com.contextengine.mcp.tool;

import com.contextengine.application.usecase.SearchUseCase;
import com.contextengine.mcp.McpConstants;

/**
 * MCP tool for executing natural-language search query lookups under the core_search namespace.
 *
 * <p>Architecture Reference: srs_search_query_fr015.pdf / srs_ce_fr020.pdf Page 5
 * Responsibility: Route core_search tool executions to the underlying SearchTool logic.
 * Dependencies: {@link SearchUseCase}, {@link McpConstants}
 * Future Usage: Exposes core_search directly to external LLM clients calling core_search.
 */
public class CoreSearchTool extends SearchTool {

    /**
     * Constructs the CoreSearchTool.
     *
     * @param searchUseCase the search use case abstraction
     */
    public CoreSearchTool(SearchUseCase searchUseCase) {
        super(searchUseCase);
    }

    @Override
    public String getName() {
        return McpConstants.TOOL_CORE_SEARCH;
    }
}
