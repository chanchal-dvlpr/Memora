package com.contextengine.application.service;

import com.contextengine.application.command.RegisterProjectCommand;
import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.exception.ProjectAlreadyRegisteredException;
import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.usecase.RegisterProjectUseCase;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.service.ProjectRegistrationService;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectRegistrationFlowTest {

    private ProjectRepository projectRepository;
    private FilesystemPort filesystemPort;
    private GitPort gitPort;
    private ProjectRegistrationService registrationService;
    private InitialSnapshotGenerator initialSnapshotGenerator;
    private RegisterProjectUseCase useCase;

    private List<Project> repoStore;
    private Path existPath;

    @BeforeEach
    void setUp() {
        repoStore = new ArrayList<>();
        projectRepository = mock(ProjectRepository.class);
        
        // Mock repository lookup
        when(projectRepository.findAllActive()).thenAnswer(inv -> new ArrayList<>(repoStore));
        doAnswer(inv -> {
            Project p = inv.getArgument(0);
            repoStore.removeIf(existing -> existing.id().equals(p.id()));
            repoStore.add(p);
            return null;
        }).when(projectRepository).save(any(Project.class));

        existPath = new Path(System.getProperty("user.dir"));

        filesystemPort = mock(FilesystemPort.class);
        when(filesystemPort.exists(any(Path.class))).thenReturn(true);
        when(filesystemPort.hasReadWritePermissions(any(Path.class))).thenReturn(true);

        gitPort = mock(GitPort.class);
        registrationService = new ProjectRegistrationService();
        initialSnapshotGenerator = mock(InitialSnapshotGenerator.class);

        useCase = new RegisterProjectUseCase(
            projectRepository,
            filesystemPort,
            registrationService,
            initialSnapshotGenerator
        );
    }

    @Test
    void testFirstTimeRegistrationSuccess() {
        RegisterProjectCommand command = new RegisterProjectCommand(existPath, "vscode-extension", List.of());

        ApplicationResult<ProjectDto> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().title()).isEqualTo("vscode-extension");
        
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(initialSnapshotGenerator, times(1)).generateInitialSnapshot(any(Project.class));
    }

    @Test
    void testDuplicateRegistrationFailsGracefully() {
        // Register first time
        RegisterProjectCommand command1 = new RegisterProjectCommand(existPath, "vscode-extension", List.of());
        ApplicationResult<ProjectDto> result1 = useCase.execute(command1);
        assertThat(result1.isSuccess()).isTrue();

        // Save project to the store
        Project project = registrationService.registerProject(existPath, "vscode-extension", List.of());
        repoStore.add(project);

        // Register the duplicate root path second time
        RegisterProjectCommand command2 = new RegisterProjectCommand(existPath, "vscode-extension-different-title", List.of());
        ApplicationResult<ProjectDto> result2 = useCase.execute(command2);

        assertThat(result2.isFailure()).isTrue();
        assertThat(result2.error().get()).isInstanceOf(ProjectAlreadyRegisteredException.class);
        assertThat(result2.error().get().getMessage()).contains("Project already registered");
    }

    @Test
    void testDuplicateRegistrationAfterRestart() {
        // Simulates project saved in database from previous run
        Project project = registrationService.registerProject(existPath, "vscode-extension", List.of());
        repoStore.add(project);

        // Register the duplicate root path second time after restart
        RegisterProjectCommand command = new RegisterProjectCommand(existPath, "vscode-extension", List.of());
        ApplicationResult<ProjectDto> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().get()).isInstanceOf(ProjectAlreadyRegisteredException.class);
        assertThat(result.error().get().getMessage()).contains("Project already registered");
    }

    @Test
    void testDuplicateRegistrationUsingExtensionPayload() {
        // First registration
        RegisterProjectCommand command1 = new RegisterProjectCommand(existPath, "vscode-extension", List.of());
        ApplicationResult<ProjectDto> result1 = useCase.execute(command1);
        assertThat(result1.isSuccess()).isTrue();

        // Save project
        Project project = registrationService.registerProject(existPath, "vscode-extension", List.of());
        repoStore.add(project);

        // Duplicate registration using extension payload
        RegisterProjectCommand command2 = new RegisterProjectCommand(existPath, "vscode-extension", List.of());
        ApplicationResult<ProjectDto> result2 = useCase.execute(command2);

        assertThat(result2.isFailure()).isTrue();
        assertThat(result2.error().get()).isInstanceOf(ProjectAlreadyRegisteredException.class);
    }
}
