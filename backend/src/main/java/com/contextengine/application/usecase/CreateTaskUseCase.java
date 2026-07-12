package com.contextengine.application.usecase;

import com.contextengine.application.command.CreateTaskCommand;
import com.contextengine.application.dto.TaskDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.TaskMapper;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Task;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.TaskId;
import java.util.List;
import java.util.Objects;

/**
 * Use case coordinating the creation of a task linked under a project features map.
 * <p>
 * Bounded Context: Project Management
 * Related Command: CreateTaskCommand
 * Related Bounded Context / Aggregate: Project (Project Management Aggregate)
 * </p>
 */
public class CreateTaskUseCase implements UseCase<CreateTaskCommand, ApplicationResult<TaskDto>> {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a CreateTaskUseCase.
     *
     * @param projectRepository repository interface
     */
    public CreateTaskUseCase(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    @Override
    public ApplicationResult<TaskDto> execute(CreateTaskCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");

            Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ApplicationException("Project not found: " + command.projectId().value()));

            Task task = new Task(
                TaskId.generate(),
                command.featureId(),
                command.projectId(),
                command.description(),
                command.priority(),
                List.of()
            );

            project.addTask(task);
            projectRepository.save(project);

            return ApplicationResult.success(TaskMapper.toDto(task));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Task creation failed", e));
        }
    }
}
