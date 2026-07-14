package com.contextengine.security.isolation;

import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Component executing physical boundary validations to contain operations inside project root directories.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Workspace Isolation Boundary)
 * Responsibility: Resolve canonical paths, reject directory traversal attacks (e.g., using {@code ..}), and verify sub-path containment.
 * Dependencies: {@link SecurityConstants}, {@link SecurityException}
 * Future Usage: Bound to FileSystemPort adapters or application services to shield disk operations.
 * </p>
 */
public class WorkspaceBoundaryValidator {

    /**
     * Asserts that a target path is physically contained within the project root folder.
     *
     * @param rootPath the registered absolute project root directory
     * @param targetPath the file or directory path requested for the operation
     * @throws SecurityException if the target path escapes the project root or contains boundary violations
     */
    public void validateBoundary(Path rootPath, Path targetPath) {
        Objects.requireNonNull(rootPath, "Root path must not be null");
        Objects.requireNonNull(targetPath, "Target path must not be null");

        try {
            // Resolve canonical paths to evaluate symlinks and dot-dots ("..")
            File rootFile = rootPath.toFile().getCanonicalFile();
            File targetFile = targetPath.toFile().getCanonicalFile();

            String rootCanonical = rootFile.getPath();
            String targetCanonical = targetFile.getPath();

            // Root canonical path separator suffix ensures clean prefix checking
            String prefix = rootCanonical.endsWith(File.separator) ? rootCanonical : rootCanonical + File.separator;

            if (!targetCanonical.equals(rootCanonical) && !targetCanonical.startsWith(prefix)) {
                throw new SecurityException(
                    SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                    "Boundary breach: Target path escapes project root directory. Root: " + rootCanonical + ", Target: " + targetCanonical
                );
            }
        } catch (IOException e) {
            throw new SecurityException(
                SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                "Boundary check failed: Unable to resolve canonical paths",
                java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "")
            );
        }
    }
}
