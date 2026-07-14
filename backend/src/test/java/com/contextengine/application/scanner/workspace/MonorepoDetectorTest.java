package com.contextengine.application.scanner.workspace;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.domain.valueobject.Path;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MonorepoDetectorTest {

    @Test
    void testDetectPnpmWorkspace() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        String yamlContent = "packages:\n  - 'packages/*'\n  - 'apps/*'\n";
        when(port.readFile(new Path("/root/pnpm-workspace.yaml"))).thenReturn(yamlContent);
        when(port.readFile(new Path("/root/packages/module-a/package.json"))).thenReturn("{\"name\": \"module-a\"}");

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("pnpm-workspace.yaml", "/root/pnpm-workspace.yaml", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("packages/module-a/package.json", "/root/packages/module-a/package.json", 100L, Instant.now(), "FILE", SupportedLanguage.JAVASCRIPT)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.PNPM, descriptor.workspaceType());
        assertEquals(1, descriptor.detectedModules().size());
        WorkspaceModule mod = descriptor.detectedModules().get(0);
        assertEquals("module-a", mod.moduleName());
        assertEquals("packages/module-a", mod.relativePath());
        assertEquals("pnpm", mod.buildSystem());
        assertNotNull(mod.moduleId());
    }

    @Test
    void testDetectNpmWorkspace() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        String rootJson = "{\n  \"name\": \"root\",\n  \"workspaces\": [\n    \"packages/*\"\n  ]\n}";
        when(port.readFile(new Path("/root/package.json"))).thenReturn(rootJson);
        when(port.readFile(new Path("/root/packages/module-b/package.json"))).thenReturn("{\"name\": \"module-b\"}");

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("package.json", "/root/package.json", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("packages/module-b/package.json", "/root/packages/module-b/package.json", 100L, Instant.now(), "FILE", SupportedLanguage.JAVASCRIPT)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.NPM, descriptor.workspaceType());
        assertEquals(1, descriptor.detectedModules().size());
        WorkspaceModule mod = descriptor.detectedModules().get(0);
        assertEquals("module-b", mod.moduleName());
        assertEquals("packages/module-b", mod.relativePath());
        assertEquals("npm", mod.buildSystem());
    }

    @Test
    void testDetectLernaWorkspace() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        String lernaJson = "{\n  \"packages\": [\n    \"packages/*\"\n  ]\n}";
        when(port.readFile(new Path("/root/lerna.json"))).thenReturn(lernaJson);
        when(port.readFile(new Path("/root/packages/module-c/package.json"))).thenReturn("{\"name\": \"module-c\"}");

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("lerna.json", "/root/lerna.json", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("packages/module-c/package.json", "/root/packages/module-c/package.json", 100L, Instant.now(), "FILE", SupportedLanguage.JAVASCRIPT)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.LERNA, descriptor.workspaceType());
        assertEquals(1, descriptor.detectedModules().size());
        WorkspaceModule mod = descriptor.detectedModules().get(0);
        assertEquals("module-c", mod.moduleName());
        assertEquals("packages/module-c", mod.relativePath());
        assertEquals("lerna", mod.buildSystem());
    }

    @Test
    void testDetectRushWorkspace() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        String rushJson = "{\n  \"projects\": [\n    {\n      \"packageName\": \"module-d\",\n      \"projectFolder\": \"apps/module-d\"\n    }\n  ]\n}";
        when(port.readFile(new Path("/root/rush.json"))).thenReturn(rushJson);
        when(port.readFile(new Path("/root/apps/module-d/package.json"))).thenReturn("{\"name\": \"module-d\"}");

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("rush.json", "/root/rush.json", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("apps/module-d/package.json", "/root/apps/module-d/package.json", 100L, Instant.now(), "FILE", SupportedLanguage.JAVASCRIPT)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.RUSH, descriptor.workspaceType());
        assertEquals(1, descriptor.detectedModules().size());
        WorkspaceModule mod = descriptor.detectedModules().get(0);
        assertEquals("module-d", mod.moduleName());
        assertEquals("apps/module-d", mod.relativePath());
        assertEquals("rush", mod.buildSystem());
    }

    @Test
    void testDetectNxWorkspace() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        when(port.readFile(new Path("/root/nx.json"))).thenReturn("{}");
        when(port.readFile(new Path("/root/packages/module-e/project.json"))).thenReturn("{\"name\": \"module-e\"}");

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("nx.json", "/root/nx.json", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("packages/module-e/project.json", "/root/packages/module-e/project.json", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.NX, descriptor.workspaceType());
        assertEquals(1, descriptor.detectedModules().size());
        WorkspaceModule mod = descriptor.detectedModules().get(0);
        assertEquals("module-e", mod.moduleName());
        assertEquals("packages/module-e", mod.relativePath());
        assertEquals("nx", mod.buildSystem());
    }

    @Test
    void testDetectTurboWorkspace() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        String rootJson = "{\n  \"workspaces\": [\n    \"packages/*\"\n  ]\n}";
        when(port.readFile(new Path("/root/package.json"))).thenReturn(rootJson);
        when(port.readFile(new Path("/root/turbo.json"))).thenReturn("{}");
        when(port.readFile(new Path("/root/packages/module-f/package.json"))).thenReturn("{\"name\": \"module-f\"}");

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("package.json", "/root/package.json", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("turbo.json", "/root/turbo.json", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("packages/module-f/package.json", "/root/packages/module-f/package.json", 100L, Instant.now(), "FILE", SupportedLanguage.JAVASCRIPT)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.TURBO, descriptor.workspaceType());
        assertEquals(1, descriptor.detectedModules().size());
        WorkspaceModule mod = descriptor.detectedModules().get(0);
        assertEquals("module-f", mod.moduleName());
        assertEquals("packages/module-f", mod.relativePath());
        assertEquals("npm", mod.buildSystem()); // Turbo delegates to NPM workspace lookup
    }

    @Test
    void testDetectMavenMultiModule() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        String parentPom = "<project>\n  <modules>\n    <module>module-g</module>\n  </modules>\n</project>";
        when(port.readFile(new Path("/root/pom.xml"))).thenReturn(parentPom);
        when(port.readFile(new Path("/root/module-g/pom.xml"))).thenReturn("<project><artifactId>module-g-artifact</artifactId></project>");

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("pom.xml", "/root/pom.xml", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED),
            new ScanCandidate("module-g/pom.xml", "/root/module-g/pom.xml", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.MAVEN, descriptor.workspaceType());
        assertEquals(1, descriptor.detectedModules().size());
        WorkspaceModule mod = descriptor.detectedModules().get(0);
        assertEquals("module-g-artifact", mod.moduleName());
        assertEquals("module-g", mod.relativePath());
        assertEquals("maven", mod.buildSystem());
    }

    @Test
    void testDetectGradleMultiProject() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        String settingsGradle = "rootProject.name = 'root'\ninclude 'module-h'\ninclude ':sub:child'\n";
        when(port.readFile(new Path("/root/settings.gradle"))).thenReturn(settingsGradle);

        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("settings.gradle", "/root/settings.gradle", 100L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.GRADLE, descriptor.workspaceType());
        assertEquals(2, descriptor.detectedModules().size());
        WorkspaceModule mod1 = descriptor.detectedModules().get(0);
        assertEquals("module-h", mod1.moduleName());
        assertEquals("module-h", mod1.relativePath());
        assertEquals("gradle", mod1.buildSystem());

        WorkspaceModule mod2 = descriptor.detectedModules().get(1);
        assertEquals("sub:child", mod2.moduleName());
        assertEquals("sub/child", mod2.relativePath());
        assertEquals("gradle", mod2.buildSystem());
    }

    @Test
    void testDetectSingleProject() {
        FilesystemPort port = Mockito.mock(FilesystemPort.class);
        List<ScanCandidate> candidates = Arrays.asList(
            new ScanCandidate("src/main/java/App.java", "/root/src/main/java/App.java", 100L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        MonorepoDetector detector = new MonorepoDetector();
        MonorepoDescriptor descriptor = detector.detect("/root", candidates, port);

        assertEquals(WorkspaceType.NONE, descriptor.workspaceType());
        assertTrue(descriptor.detectedModules().isEmpty());
        assertTrue(descriptor.moduleNames().isEmpty());
        assertTrue(descriptor.modulePaths().isEmpty());
        assertEquals("/root", descriptor.rootPath());
    }
}
