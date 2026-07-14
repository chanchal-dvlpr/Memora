package com.contextengine.application.usecase;

import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.repository.ProjectRepository;
import java.util.Objects;

/**
 * Use case coordinating project unregistration and index cleanup.
 */
public class RemoveProjectUseCase implements UseCase<ProjectId, ApplicationResult<Boolean>> {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a RemoveProjectUseCase.
     *
     * @param projectRepository project repository interface
     */
    public RemoveProjectUseCase(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    @Override
    public ApplicationResult<Boolean> execute(ProjectId projectId) {
        try {
            Objects.requireNonNull(projectId, "ProjectId must not be null");
            projectRepository.findById(projectId)
                .orElseThrow(() -> new com.contextengine.application.exception.ProjectNotFoundException("Project not found with ID: " + projectId.value()));
            projectRepository.remove(projectId);
            return ApplicationResult.success(true);
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Failed to remove project", e));
        }
    }
}
