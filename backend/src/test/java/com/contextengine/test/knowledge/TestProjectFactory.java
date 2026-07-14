package com.contextengine.test.knowledge;

import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.application.scanner.dependency.ProjectDependency;

import java.time.Instant;
import java.util.*;

/**
 * Reusable test factory generating mock project scanner outputs.
 */
public class TestProjectFactory {

    public static Collection<ScanCandidate> createEmptyWorkspace() {
        return Collections.emptyList();
    }

    public static Collection<ScanCandidate> createSingleFileProject() {
        return List.of(
            new ScanCandidate(
                "src/Main.java",
                "/workspace/src/Main.java",
                500L,
                Instant.now(),
                "FILE",
                SupportedLanguage.JAVA
            )
        );
    }

    public static Collection<ScanCandidate> createMultiModuleProject() {
        return List.of(
            new ScanCandidate("pom.xml", "/workspace/pom.xml", 800L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("module-a/pom.xml", "/workspace/module-a/pom.xml", 400L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("module-a/src/ServiceA.java", "/workspace/module-a/src/ServiceA.java", 1200L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("module-b/pom.xml", "/workspace/module-b/pom.xml", 400L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("module-b/src/ServiceB.java", "/workspace/module-b/src/ServiceB.java", 1500L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );
    }

    public static Collection<ScanCandidate> createLargeWorkspace(int fileCount) {
        List<ScanCandidate> candidates = new ArrayList<>(fileCount);
        Instant now = Instant.now();
        for (int i = 0; i < fileCount; i++) {
            candidates.add(
                new ScanCandidate(
                    "src/File" + i + ".java",
                    "/workspace/src/File" + i + ".java",
                    300L,
                    now,
                    "FILE",
                    SupportedLanguage.JAVA
                )
            );
        }
        return candidates;
    }

    public static Collection<SourceSymbol> createSymbolsForFile(String filePath, int count) {
        List<SourceSymbol> symbols = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            symbols.add(
                new SourceSymbol(
                    "Symbol" + i,
                    "CLASS",
                    filePath,
                    1,
                    10,
                    Map.of("kind", "CLASS")
                )
            );
        }
        return symbols;
    }

    public static Collection<ProjectDependency> createDependencyHeavyProject(int count) {
        List<ProjectDependency> deps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            deps.add(new ProjectDependency("dep-package-" + i, "1.0." + i, "MAVEN", "COMPILE"));
        }
        return deps;
    }
}
