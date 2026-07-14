package com.contextengine.application.usecase;

import com.contextengine.application.dto.KnowledgeNodeDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.KnowledgeNodeMapper;
import com.contextengine.application.query.SearchProjectQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.service.SearchService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Use case coordinating search lookups over the engineering memory knowledge graph.
 *
 * <p>Architecture Reference: srs_search_query_fr015.pdf / Application Layer Design
 * Responsibility: Query project nodes from the graph repository, delegate searching to the domain service, and map results to DTOs.
 * Dependencies: {@link SearchService}, {@link KnowledgeGraphRepository}, {@link SearchProjectQuery}, {@link KnowledgeNodeDto}
 * Future Usage: Bound to search adapter boundaries, MCP search tools, and query REST controllers.
 */
public class SearchUseCase implements UseCase<SearchProjectQuery, ApplicationResult<List<KnowledgeNodeDto>>> {

    private final SearchService searchService;
    private final KnowledgeGraphRepository graphRepository;

    /**
     * Constructs a SearchUseCase.
     *
     * @param searchService the search domain service
     * @param graphRepository the knowledge graph repository interface
     */
    public SearchUseCase(SearchService searchService, KnowledgeGraphRepository graphRepository) {
        this.searchService = Objects.requireNonNull(searchService, "SearchService must not be null");
        this.graphRepository = Objects.requireNonNull(graphRepository, "KnowledgeGraphRepository must not be null");
    }

    @Override
    public ApplicationResult<List<KnowledgeNodeDto>> execute(SearchProjectQuery query) {
        try {
            Objects.requireNonNull(query, "SearchProjectQuery must not be null");

            Collection<KnowledgeNode> catalog = graphRepository.findNodesByProject(query.projectId());
            Collection<KnowledgeNode> matched = searchService.executeSearch(query.searchQuery(), catalog);

            List<KnowledgeNodeDto> results = matched.stream()
                .map(KnowledgeNodeMapper::toDto)
                .collect(Collectors.toList());

            return ApplicationResult.success(results);
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Search operation failed", e));
        }
    }
}
