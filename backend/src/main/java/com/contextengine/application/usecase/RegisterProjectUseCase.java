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
import com.contextengine.application.exception.ProjectAlreadyRegisteredException;
import com.contextengine.application.exception.ProjectPathOverlapsException;
import com.contextengine.application.exception.DirectoryAccessDeniedApplicationException;
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
    private final com.contextengine.application.service.InitialSnapshotGenerator initialSnapshotGenerator;

    /**
     * Constructs a RegisterProjectUseCase.
     *
     * @param projectRepository repository interface
     * @param filesystemPort outbound port for hardware file system queries
     * @param registrationService domain registration service
     * @param initialSnapshotGenerator generator for initial project snapshot
     */
    public RegisterProjectUseCase(
        ProjectRepository projectRepository,
        FilesystemPort filesystemPort,
        ProjectRegistrationService registrationService,
        com.contextengine.application.service.InitialSnapshotGenerator initialSnapshotGenerator
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
        this.registrationService = Objects.requireNonNull(registrationService, "ProjectRegistrationService must not be null");
        this.initialSnapshotGenerator = Objects.requireNonNull(initialSnapshotGenerator, "InitialSnapshotGenerator must not be null");
    }

    @Override
    public ApplicationResult<ProjectDto> execute(RegisterProjectCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");
            
            if (!filesystemPort.exists(command.absoluteRootPath())) {
                return ApplicationResult.failure(new DirectoryAccessDeniedApplicationException(
                    "Project directory does not exist: " + command.absoluteRootPath().value()));
            }
            if (!filesystemPort.hasReadWritePermissions(command.absoluteRootPath())) {
                return ApplicationResult.failure(new DirectoryAccessDeniedApplicationException(
                    "Missing read/write permissions for directory: " + command.absoluteRootPath().value()));
            }

            // Explicitly check for exact duplicate registration before registry overlapping checks
            boolean duplicateExists = projectRepository.findAllActive().stream()
                .anyMatch(p -> p.rootDirectory().value().equals(command.absoluteRootPath().value()));
            if (duplicateExists) {
                return ApplicationResult.failure(new ProjectAlreadyRegisteredException(
                    "Project already registered with root path: " + command.absoluteRootPath().value()));
            }

            Project project = registrationService.registerProject(
                command.absoluteRootPath(),
                command.projectTitle(),
                projectRepository.findAllActive()
            );

            projectRepository.save(project);

            // Synchronously trigger lightweight scan and snapshot generation
            initialSnapshotGenerator.generateInitialSnapshot(project);

            return ApplicationResult.success(ProjectMapper.toDto(project));
        } catch (com.contextengine.domain.service.OverlappingProjectException e) {
            return ApplicationResult.failure(new ProjectPathOverlapsException(e.getMessage(), e));
        } catch (com.contextengine.domain.service.DirectoryAccessDeniedException e) {
            return ApplicationResult.failure(new DirectoryAccessDeniedApplicationException(e.getMessage(), e));
        } catch (ProjectAlreadyRegisteredException | ProjectPathOverlapsException | DirectoryAccessDeniedApplicationException e) {
            return ApplicationResult.failure(e);
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Project registration failed", e));
        }
    }
}
