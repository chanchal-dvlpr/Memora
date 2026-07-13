package com.contextengine.application.usecase;

import com.contextengine.application.command.ScanProjectCommand;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.scanner.ScannerEngine;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Workspace;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.Path;
import java.util.List;
import java.util.Objects;

/**
 * Use case coordinating project file traversal, indexing triggers, and Git metadata bindings.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Command: ScanProjectCommand
 * Related Bounded Context / Aggregate: Project / Workspace
 * </p>
 */
public class ScanProjectUseCase implements UseCase<ScanProjectCommand, ApplicationResult<Boolean>> {

    private final ProjectRepository projectRepository;
    private final FilesystemPort filesystemPort;
    private final GitPort gitPort;
    private final ScannerEngine scannerEngine;

    /**
     * Constructs a ScanProjectUseCase.
     *
     * @param projectRepository repository interface
     * @param filesystemPort outbound port for physical file reads
     * @param gitPort outbound port for local Git VCS interactions
     * @param scannerEngine application scanner engine
     */
    public ScanProjectUseCase(
        ProjectRepository projectRepository,
        FilesystemPort filesystemPort,
        GitPort gitPort,
        ScannerEngine scannerEngine
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
        this.gitPort = Objects.requireNonNull(gitPort, "GitPort must not be null");
        this.scannerEngine = Objects.requireNonNull(scannerEngine, "ScannerEngine must not be null");
    }

    @Override
    public ApplicationResult<Boolean> execute(ScanProjectCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");

            Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ApplicationException("Project not found: " + command.projectId().value()));

            Workspace workspace = project.workspace();
            if (workspace == null) {
                return ApplicationResult.failure(new ApplicationException("Project workspace has not been initialized"));
            }

            scannerEngine.scan(project, command.deep() ? "FULL" : "INCREMENTAL");

            if (gitPort.isGitRepository(project.rootDirectory())) {
                String branch = gitPort.getActiveBranch(project.rootDirectory());
                String commit = gitPort.getLatestCommitHash(project.rootDirectory());
                workspace.updateGitMetadata(branch, commit);
            }

            projectRepository.save(project);

            return ApplicationResult.success(true);
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Workspace scan failed", e));
        }
    }
}
