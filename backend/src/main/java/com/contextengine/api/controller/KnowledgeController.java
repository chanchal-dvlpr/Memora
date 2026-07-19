package com.contextengine.api.controller;

import com.contextengine.application.dto.KnowledgeNodeDto;
import com.contextengine.application.query.SearchProjectQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.usecase.SearchUseCase;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SearchQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller coordinating search lookups and query processing over the engineering knowledge base.
 */
@RestController
@Tag(name = "Knowledge Search", description = "Endpoints for searching codebase symbols, files, and relationships.")
public class KnowledgeController {

    private final SearchUseCase searchUseCase;

    /**
     * Constructs a KnowledgeController.
     *
     * @param searchUseCase the search use case dependency
     */
    public KnowledgeController(SearchUseCase searchUseCase) {
        this.searchUseCase = Objects.requireNonNull(searchUseCase, "SearchUseCase must not be null");
    }

    public record KnowledgeQueryRequest(
        String projectId,
        String query,
        Integer limit
    ) {}

    public record KnowledgeDocument(
        String id,
        String title,
        String content,
        Double score
    ) {}

    public record KnowledgeQueryResponse(
        List<KnowledgeDocument> documents
    ) {}

    /**
     * Exposes the endpoint for querying workspace knowledge base files/symbols.
     */
    @PostMapping("/api/v1/knowledge/query")
    @Operation(summary = "Search project knowledge", description = "Queries codebase index files and match tokens/symbols.")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully.")
    public ResponseEntity<KnowledgeQueryResponse> queryKnowledge(
        @RequestBody KnowledgeQueryRequest request
    ) {
        if (request == null || request.projectId() == null || request.query() == null) {
            return ResponseEntity.badRequest().build();
        }
        int maxResults = request.limit() != null ? request.limit() : 10;
        if (maxResults <= 0) {
            maxResults = 10;
        }

        SearchProjectQuery query = new SearchProjectQuery(
            new ProjectId(UUID.fromString(request.projectId())),
            new SearchQuery(request.query(), false, Metadata.empty(), maxResults)
        );

        ApplicationResult<List<KnowledgeNodeDto>> result = searchUseCase.execute(query);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Search failed"));
        }

        List<KnowledgeDocument> documents = result.value().orElse(Collections.emptyList()).stream()
            .map(node -> new KnowledgeDocument(
                node.id(),
                node.attributes().getOrDefault("name", "untitled"),
                node.attributes().getOrDefault("content", ""),
                1.0
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(new KnowledgeQueryResponse(documents));
    }
}
