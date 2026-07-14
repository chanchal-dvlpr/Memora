package com.contextengine.api.service;

import com.contextengine.api.mapper.ContextResponseMapper;
import com.contextengine.api.request.GenerateContextRequest;
import com.contextengine.api.response.ContextResponse;
import com.contextengine.application.command.GenerateContextCommand;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.query.GetLatestSnapshotQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.domain.service.FormatEnum;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SearchQuery;
import com.contextengine.domain.valueobject.TokenBudget;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Service orchestrating HTTP presentation concerns for Context assembly and snapshot queries.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.5 (Context Assembly Engine)
 * </p>
 */
@Service
public class ContextRestService {

    private final ContextApplicationService contextService;

    /**
     * Constructs a ContextRestService.
     *
     * @param contextService application layer context service dependency
     */
    public ContextRestService(ContextApplicationService contextService) {
        this.contextService = Objects.requireNonNull(contextService, "ContextApplicationService must not be null");
    }

    /**
     * Orchestrates context compilation requests and maps results to presentation models.
     *
     * @param request context compilation request params
     * @return context response payload
     */
    public ContextResponse compileContextScope(GenerateContextRequest request) {
        Objects.requireNonNull(request, "Request must not be null");
        ProjectId projectId = new ProjectId(UUID.fromString(request.getProjectId()));
        
        String term = request.getQuery() != null && !request.getQuery().trim().isEmpty() ? request.getQuery() : "default-query";
        SearchQuery query = new SearchQuery(term, false, com.contextengine.domain.valueobject.Metadata.empty(), 100);

        NodeId focusNode = request.getFocusFile() != null && !request.getFocusFile().isEmpty()
            ? NodeId.generate()
            : NodeId.generate();

        FormatEnum formatVal = FormatEnum.MARKDOWN;
        if (request.getFormat() != null) {
            try {
                formatVal = FormatEnum.valueOf(request.getFormat().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Keep default MARKDOWN
            }
        }

        GenerateContextCommand command = new GenerateContextCommand(
            projectId,
            query,
            focusNode,
            new TokenBudget(request.getMaxTokenBudget()),
            formatVal
        );

        ApplicationResult<ContextSnapshotDto> result = contextService.generateContext(command);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Context generation failed"));
        }

        return ContextResponseMapper.toResponse(result.value().orElseThrow());
    }

    /**
     * Orchestrates latest snapshot lookup queries.
     *
     * @param projectId project UUID string
     * @return context response payload
     */
    public ContextResponse getLatestSnapshot(String projectId) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        GetLatestSnapshotQuery query = new GetLatestSnapshotQuery(new ProjectId(UUID.fromString(projectId)));
        ApplicationResult<ContextSnapshotDto> result = contextService.getLatestSnapshot(query);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Snapshot retrieval failed"));
        }

        return ContextResponseMapper.toResponse(result.value().orElseThrow());
    }
}
