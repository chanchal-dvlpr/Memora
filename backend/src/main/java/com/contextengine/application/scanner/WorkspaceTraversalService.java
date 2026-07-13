package com.contextengine.application.scanner;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.domain.valueobject.Path;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service responsible for recursive directory traversal using the FilesystemPort.
 * Enforces boundary confinement (SEC-005) and respects project exclusions.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class WorkspaceTraversalService {

    private final FilesystemPort filesystemPort;

    /**
     * Constructs a WorkspaceTraversalService.
     *
     * @param filesystemPort outbound port for physical filesystem operations
     */
    public WorkspaceTraversalService(FilesystemPort filesystemPort) {
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
    }

    /**
     * Performs a recursive traversal using the FilesystemPort starting from the canonical root.
     *
     * @param context active scanner context
     * @param ignoreRuleProcessor filter processor to skip paths
     * @return list of traversed absolute java.nio.file.Path objects
     */
    public List<java.nio.file.Path> traverse(ScannerContext context, IgnoreRuleProcessor ignoreRuleProcessor) {
        Objects.requireNonNull(context, "ScannerContext must not be null");
        Objects.requireNonNull(ignoreRuleProcessor, "IgnoreRuleProcessor must not be null");

        Path root = context.getCanonicalRoot();
        java.nio.file.Path rootPath = Paths.get(root.value());

        // Resolve absolute canonical root for boundary verification
        java.nio.file.Path canonicalRootPath;
        try {
            if (Files.exists(rootPath)) {
                canonicalRootPath = rootPath.toRealPath();
            } else {
                canonicalRootPath = rootPath.toAbsolutePath().normalize();
            }
        } catch (IOException e) {
            throw new ScannerException("ERR_SCAN_ROOT_NOT_FOUND: Failed to resolve project canonical root", e);
        }

        // Get relative files from filesystemPort applying raw ignore patterns
        List<Path> relativePaths = filesystemPort.listFiles(root, ignoreRuleProcessor.getRawExclusions());
        List<java.nio.file.Path> traversedPaths = new ArrayList<>();

        for (Path relPath : relativePaths) {
            java.nio.file.Path absolute = canonicalRootPath.resolve(relPath.value()).toAbsolutePath().normalize();

            // Enforce Root Confinement Boundary (SEC-005)
            if (absolute.startsWith(canonicalRootPath)) {
                // Check ignore rules one final time on the relative path string
                String relPathStr = canonicalRootPath.relativize(absolute).toString().replace('\\', '/');
                if (!ignoreRuleProcessor.shouldIgnore(relPathStr, false)) {
                    traversedPaths.add(absolute);
                }
            }
        }

        // Track a dummy directory increment in session to represent root traversal
        context.getSession().incrementDirectories();

        return traversedPaths;
    }
}
