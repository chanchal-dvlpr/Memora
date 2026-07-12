package com.contextengine.application.usecase;

import com.contextengine.application.command.CreateFeatureCommand;
import com.contextengine.application.dto.FeatureDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.FeatureMapper;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.FeatureId;
import java.util.Objects;

/**
 * Use case coordinating the creation and linkage of a milestone feature to a Project.
 * <p>
 * Bounded Context: Project Management
 * Related Command: CreateFeatureCommand
 * Related Bounded Context / Aggregate: Project (Project Management Aggregate)
 * </p>
 */
public class CreateFeatureUseCase implements UseCase<CreateFeatureCommand, ApplicationResult<FeatureDto>> {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a CreateFeatureUseCase.
     *
     * @param projectRepository repository interface
     */
    public CreateFeatureUseCase(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    @Override
    public ApplicationResult<FeatureDto> execute(CreateFeatureCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");

            Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ApplicationException("Project not found: " + command.projectId().value()));

            Feature feature = new Feature(
                FeatureId.generate(),
                command.projectId(),
                command.title(),
                command.priority()
            );

            project.addFeature(feature);
            projectRepository.save(project);

            return ApplicationResult.success(FeatureMapper.toDto(feature));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Feature creation failed", e));
        }
    }
}
