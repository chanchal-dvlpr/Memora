package com.contextengine.application.service;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitialSnapshotGeneratorTest {

    private ContextRepository contextRepository;
    private FilesystemPort filesystemPort;
    private GitPort gitPort;
    private InitialSnapshotGenerator generator;

    @BeforeEach
    void setUp() {
        contextRepository = mock(ContextRepository.class);
        filesystemPort = mock(FilesystemPort.class);
        gitPort = mock(GitPort.class);
        generator = new InitialSnapshotGenerator(contextRepository, filesystemPort, gitPort);
    }

    @Test
    void testGenerateInitialSnapshotSuccess() {
        ProjectId projectId = ProjectId.generate();
        Path root = new Path("/workspace/test-project");
        Project project = new Project(projectId, root, "test-project");

        List<Path> mockFiles = List.of(
            new Path("src/Main.java"),
            new Path("src/utils/Helper.kt"),
            new Path("package.json"),
            new Path("README.md")
        );

        when(filesystemPort.listFiles(any(Path.class), any())).thenReturn(mockFiles);
        when(filesystemPort.readFile(any(Path.class))).thenReturn("# Test Project\nThis is a test readme.");
        when(gitPort.isGitRepository(any(Path.class))).thenReturn(true);
        when(contextRepository.findLatestSnapshotForProject(any(ProjectId.class))).thenReturn(Optional.empty());

        ContextSnapshot snapshot = generator.generateInitialSnapshot(project);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.projectId()).isEqualTo(projectId);
        assertThat(snapshot.version().value()).isEqualTo(1);
        assertThat(snapshot.payload()).contains("# Memora Context Snapshot");
        assertThat(snapshot.payload()).contains("Name: test-project");
        assertThat(snapshot.payload()).contains("- Java");
        assertThat(snapshot.payload()).contains("- Kotlin");
        assertThat(snapshot.payload()).contains("- npm (package.json detected)");
        assertThat(snapshot.payload()).contains("- Git repository detected");
        assertThat(snapshot.payload()).contains("This is a test readme.");

        verify(contextRepository, times(1)).save(any());
    }
}
