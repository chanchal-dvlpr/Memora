package com.contextengine.application.scanner.dependency;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.domain.valueobject.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Identifies project manifests in a workspace, reads their content via FilesystemPort,
 * and extracts project-level dependencies.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class DependencyScanner {

    private final FilesystemPort filesystemPort;
    private final ManifestParser manifestParser;

    /**
     * Constructs a DependencyScanner.
     *
     * @param filesystemPort file system port for reading content
     * @param manifestParser parser for specific manifests
     */
    public DependencyScanner(FilesystemPort filesystemPort, ManifestParser manifestParser) {
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
        this.manifestParser = Objects.requireNonNull(manifestParser, "ManifestParser must not be null");
    }

    /**
     * Scans candidates for manifests and extracts all project dependencies.
     *
     * @param candidates discovered workspace candidates
     * @return collection of extracted dependencies
     */
    public Collection<ProjectDependency> scan(Collection<ScanCandidate> candidates) {
        Objects.requireNonNull(candidates, "Candidates must not be null");
        List<ProjectDependency> dependencies = new ArrayList<>();

        for (ScanCandidate candidate : candidates) {
            String relativePath = candidate.relativePath();
            if (isManifest(relativePath)) {
                try {
                    String content = filesystemPort.readFile(new Path(candidate.absolutePath()));
                    if (content != null && !content.isEmpty()) {
                        dependencies.addAll(manifestParser.parse(relativePath, content));
                    }
                } catch (Exception e) {
                    System.err.println("[DEPENDENCY-SCANNER] Failed to read manifest " + relativePath + ": " + e.getMessage());
                }
            }
        }

        return dependencies;
    }

    private boolean isManifest(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith("pom.xml") || lower.endsWith("package.json") || lower.endsWith("requirements.txt");
    }
}
