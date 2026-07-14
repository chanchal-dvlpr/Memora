package com.contextengine.api.service;

import com.contextengine.api.mapper.ScanResponseMapper;
import com.contextengine.api.request.ScanProjectRequest;
import com.contextengine.api.response.ScanStatusResponse;
import com.contextengine.application.command.ScanProjectCommand;
import com.contextengine.application.dto.ScanStatusDto;
import com.contextengine.application.query.GetScanStatusQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.domain.valueobject.ProjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Service orchestrating HTTP presentation concerns for codebase scanner control and status metrics.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.3 (Codebase Index Engine)
 * </p>
 */
@Service
public class ScannerRestService {

    private final ProjectApplicationService projectService;

    /**
     * Constructs a ScannerRestService.
     *
     * @param projectService application layer project service dependency
     */
    public ScannerRestService(ProjectApplicationService projectService) {
        this.projectService = Objects.requireNonNull(projectService, "ProjectApplicationService must not be null");
    }

    /**
     * Handles requests to trigger manual scans on workspaces.
     *
     * @param id project UUID string
     * @param request scan options request configuration
     * @return scanner status metrics response
     */
    public ScanStatusResponse triggerScan(String id, ScanProjectRequest request) {
        Objects.requireNonNull(id, "ID must not be null");
        Objects.requireNonNull(request, "Request must not be null");
        ProjectId projectId = new ProjectId(UUID.fromString(id));
        ScanProjectCommand command = new ScanProjectCommand(
            projectId,
            request.isDeep(),
            true
        );

        ApplicationResult<Boolean> result = projectService.scanProject(command);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Scan failed"));
        }

        GetScanStatusQuery query = new GetScanStatusQuery(projectId);
        ApplicationResult<ScanStatusDto> statusResult = projectService.getScanStatus(query);
        long filesCount = statusResult.isSuccess() ? statusResult.value().orElseThrow().filesProcessed() : 0;

        return new ScanStatusResponse(
            UUID.randomUUID().toString(),
            id,
            "COMPLETED",
            Instant.now().toString(),
            true,
            filesCount
        );
    }

    /**
     * Handles scanner progress and health queries.
     *
     * @param id project UUID string
     * @return scanner status metrics response
     */
    public ScanStatusResponse getScannerStatus(String id) {
        Objects.requireNonNull(id, "ID must not be null");
        GetScanStatusQuery query = new GetScanStatusQuery(new ProjectId(UUID.fromString(id)));
        ApplicationResult<ScanStatusDto> result = projectService.getScanStatus(query);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Retrieve scan status failed"));
        }

        return ScanResponseMapper.toResponse(result.value().orElseThrow());
    }
}
