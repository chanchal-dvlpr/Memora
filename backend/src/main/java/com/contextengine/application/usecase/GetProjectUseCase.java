package com.contextengine.application.usecase;

import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.ProjectMapper;
import com.contextengine.application.query.GetProjectQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import java.util.Objects;

/**
 * Use case coordinating retrieval of a single registered Project metadata.
 */
public class GetProjectUseCase implements UseCase<GetProjectQuery, ApplicationResult<ProjectDto>> {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a GetProjectUseCase.
     *
     * @param projectRepository project repository interface
     */
    public GetProjectUseCase(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    @Override
    public ApplicationResult<ProjectDto> execute(GetProjectQuery query) {
        try {
            Objects.requireNonNull(query, "Query must not be null");
            Project project = projectRepository.findById(query.projectId())
                .orElseThrow(() -> new com.contextengine.application.exception.ProjectNotFoundException("Project not found with ID: " + query.projectId().value()));
            return ApplicationResult.success(ProjectMapper.toDto(project));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Failed to retrieve project", e));
        }
    }
}
