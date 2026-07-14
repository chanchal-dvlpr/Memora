package com.contextengine.application.usecase;

import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.ProjectMapper;
import com.contextengine.application.query.ListProjectsQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.repository.ProjectRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Use case coordinating retrieval of all active registered projects metadata.
 */
public class ListProjectsUseCase implements UseCase<ListProjectsQuery, ApplicationResult<List<ProjectDto>>> {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a ListProjectsUseCase.
     *
     * @param projectRepository project repository interface
     */
    public ListProjectsUseCase(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    @Override
    public ApplicationResult<List<ProjectDto>> execute(ListProjectsQuery query) {
        try {
            Objects.requireNonNull(query, "Query must not be null");
            List<ProjectDto> dtos = projectRepository.findAllActive().stream()
                .map(ProjectMapper::toDto)
                .collect(Collectors.toList());
            return ApplicationResult.success(dtos);
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Failed to list active projects", e));
        }
    }
}
