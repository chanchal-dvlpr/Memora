package com.contextengine.application.scanner.validation;

import com.contextengine.application.scanner.ScannerException;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Validates project scanner inputs and environment state to ensure integrity and security.
 * Enforces project state consistency, symlink loop detection, and directory confinement boundaries.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class ScannerValidator {

    /**
     * Constructs a ScannerValidator.
     */
    public ScannerValidator() {
    }

    /**
     * Validates project state consistency.
     *
     * @param project the project instance to validate
     * @throws ScannerException if the project is archived or cannot be scanned
     */
    public void validateProjectState(Project project) {
        Objects.requireNonNull(project, "Project must not be null");
        if (project.state() == ProjectState.ARCHIVED) {
            throw new ScannerException("ERR_SCAN_PROJECT_LOCKED: Cannot scan archived project: " + project.id().value());
        }
    }

    /**
     * Enforces path boundary confinement (SEC-005).
     *
     * @param rootPath relative or absolute root path
     * @param targetPath relative or absolute target path
     * @throws ScannerException if the target path resolves outside the root confinement boundary
     */
    public void validateConfinement(String rootPath, String targetPath) {
        Objects.requireNonNull(rootPath, "RootPath must not be null");
        Objects.requireNonNull(targetPath, "TargetPath must not be null");

        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        Path target = Paths.get(targetPath).toAbsolutePath().normalize();

        if (!target.startsWith(root)) {
            throw new ScannerException("Security violation: path resolves outside the confinement boundary: " + targetPath);
        }
    }

    /**
     * Enforces symlink loop detection (SEC-004).
     *
     * @param targetPath file or directory path string
     * @throws ScannerException if a circular symbolic reference is detected
     */
    public void validateSymlink(String targetPath) {
        Objects.requireNonNull(targetPath, "TargetPath must not be null");
        Path path = Paths.get(targetPath);

        // Standard validation: check if toRealPath throws due to circular reference loops
        if (Files.isSymbolicLink(path)) {
            try {
                path.toRealPath();
            } catch (IOException e) {
                throw new ScannerException("ERR_SCAN_CIRCULAR_REFERENCE: Circular symlink loop detected: " + targetPath, e);
            }
        }
    }
}
