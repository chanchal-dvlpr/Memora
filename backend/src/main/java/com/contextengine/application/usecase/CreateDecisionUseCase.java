package com.contextengine.application.usecase;

import com.contextengine.application.command.CreateDecisionCommand;
import com.contextengine.application.dto.DecisionDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.DecisionMapper;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.DecisionId;
import java.util.Objects;

/**
 * Use case coordinating architectural decision registration under a project workspace.
 * <p>
 * Bounded Context: Project Management
 * Related Command: CreateDecisionCommand
 * Related Bounded Context / Aggregate: Project (Project Management Aggregate)
 * </p>
 */
public class CreateDecisionUseCase implements UseCase<CreateDecisionCommand, ApplicationResult<DecisionDto>> {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a CreateDecisionUseCase.
     *
     * @param projectRepository repository interface
     */
    public CreateDecisionUseCase(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    @Override
    public ApplicationResult<DecisionDto> execute(CreateDecisionCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");

            Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ApplicationException("Project not found: " + command.projectId().value()));

            Decision decision = new Decision(
                DecisionId.generate(),
                command.projectId(),
                command.title(),
                command.markdownPath()
            );

            project.addDecision(decision);
            projectRepository.save(project);

            return ApplicationResult.success(DecisionMapper.toDto(decision));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Decision logging failed", e));
        }
    }
}
