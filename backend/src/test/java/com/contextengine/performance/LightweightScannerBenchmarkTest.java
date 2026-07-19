package com.contextengine.performance;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import com.contextengine.application.service.InitialSnapshotGenerator;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LightweightScannerBenchmarkTest {

    @Test
    void testPerformance1kFiles() {
        ContextRepository contextRepository = mock(ContextRepository.class);
        FilesystemPort filesystemPort = mock(FilesystemPort.class);
        GitPort gitPort = mock(GitPort.class);
        InitialSnapshotGenerator generator = new InitialSnapshotGenerator(contextRepository, filesystemPort, gitPort);

        ProjectId projectId = ProjectId.generate();
        Path root = new Path("/workspace/benchmark-1k");
        Project project = new Project(projectId, root, "benchmark-1k");

        List<Path> mockFiles = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            mockFiles.add(new Path("src/main/java/com/benchmark/File" + i + ".java"));
        }

        when(filesystemPort.listFiles(any(Path.class), any())).thenReturn(mockFiles);
        when(filesystemPort.readFile(any(Path.class))).thenReturn("// Simple Java file content");

        long start = System.currentTimeMillis();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        generator.generateInitialSnapshot(project);

        long end = System.currentTimeMillis();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long duration = end - start;
        long memUsed = (memAfter - memBefore) / (1024 * 1024);

        System.out.println("[BENCHMARK 1K] Duration: " + duration + " ms, Memory used: " + memUsed + " MB");
        assertThat(duration).isLessThan(2000);
    }

    @Test
    void testPerformance10kFiles() {
        ContextRepository contextRepository = mock(ContextRepository.class);
        FilesystemPort filesystemPort = mock(FilesystemPort.class);
        GitPort gitPort = mock(GitPort.class);
        InitialSnapshotGenerator generator = new InitialSnapshotGenerator(contextRepository, filesystemPort, gitPort);

        ProjectId projectId = ProjectId.generate();
        Path root = new Path("/workspace/benchmark-10k");
        Project project = new Project(projectId, root, "benchmark-10k");

        List<Path> mockFiles = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            mockFiles.add(new Path("src/main/java/com/benchmark/module" + (i % 10) + "/File" + i + ".java"));
        }

        when(filesystemPort.listFiles(any(Path.class), any())).thenReturn(mockFiles);
        when(filesystemPort.readFile(any(Path.class))).thenReturn("// Simple Java file content");

        long start = System.currentTimeMillis();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        generator.generateInitialSnapshot(project);

        long end = System.currentTimeMillis();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long duration = end - start;
        long memUsed = (memAfter - memBefore) / (1024 * 1024);

        System.out.println("[BENCHMARK 10K] Duration: " + duration + " ms, Memory used: " + memUsed + " MB");
        assertThat(duration).isLessThan(5000);
    }
}
