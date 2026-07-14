package com.contextengine.application.usecase;

import com.contextengine.application.dto.ScanStatusDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.query.GetScanStatusQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Workspace;
import com.contextengine.domain.repository.ProjectRepository;
import java.util.Objects;

/**
 * Use case coordinating scan status and workspace file metrics queries.
 */
public class GetScanStatusUseCase implements UseCase<GetScanStatusQuery, ApplicationResult<ScanStatusDto>> {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a GetScanStatusUseCase.
     *
     * @param projectRepository project repository interface
     */
    public GetScanStatusUseCase(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    @Override
    public ApplicationResult<ScanStatusDto> execute(GetScanStatusQuery query) {
        try {
            Objects.requireNonNull(query, "Query must not be null");
            Project project = projectRepository.findById(query.projectId())
                .orElseThrow(() -> new com.contextengine.application.exception.ProjectNotFoundException("Project not found with ID: " + query.projectId().value()));

            Workspace workspace = project.workspace();
            long filesCount = 0;
            String branch = "";
            String commit = "";
            if (workspace != null) {
                filesCount = workspace.trackedPaths().size();
                branch = workspace.activeBranch();
                commit = workspace.activeCommitHash();
            }

            return ApplicationResult.success(new ScanStatusDto(
                project.id().value().toString(),
                workspace != null,
                filesCount,
                branch,
                commit
            ));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Failed to retrieve scanner status", e));
        }
    }
}
