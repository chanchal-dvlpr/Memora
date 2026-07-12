package com.contextengine.application.usecase;

import com.contextengine.application.command.RegisterProjectCommand;
import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.ProjectMapper;
import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.service.ProjectRegistrationService;
import java.util.Objects;

/**
 * Use case coordinating the registration and workspace initialization of a new Project.
 * <p>
 * Bounded Context: Project Management
 * Related Command: RegisterProjectCommand
 * Related Bounded Context / Aggregate: Project
 * </p>
 */
public class RegisterProjectUseCase implements UseCase<RegisterProjectCommand, ApplicationResult<ProjectDto>> {

    private final ProjectRepository projectRepository;
    private final FilesystemPort filesystemPort;
    private final ProjectRegistrationService registrationService;

    /**
     * Constructs a RegisterProjectUseCase.
     *
     * @param projectRepository repository interface
     * @param filesystemPort outbound port for hardware file system queries
     * @param registrationService domain registration service
     */
    public RegisterProjectUseCase(
        ProjectRepository projectRepository,
        FilesystemPort filesystemPort,
        ProjectRegistrationService registrationService
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
        this.registrationService = Objects.requireNonNull(registrationService, "ProjectRegistrationService must not be null");
    }

    @Override
    public ApplicationResult<ProjectDto> execute(RegisterProjectCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");
            
            if (!filesystemPort.exists(command.absoluteRootPath())) {
                return ApplicationResult.failure(new ApplicationException(
                    "Project directory does not exist: " + command.absoluteRootPath().value()));
            }
            if (!filesystemPort.hasReadWritePermissions(command.absoluteRootPath())) {
                return ApplicationResult.failure(new ApplicationException(
                    "Missing read/write permissions for directory: " + command.absoluteRootPath().value()));
            }

            Project project = registrationService.registerProject(
                command.absoluteRootPath(),
                command.projectTitle(),
                projectRepository.findAllActive()
            );

            projectRepository.save(project);

            return ApplicationResult.success(ProjectMapper.toDto(project));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Project registration failed", e));
        }
    }
}
