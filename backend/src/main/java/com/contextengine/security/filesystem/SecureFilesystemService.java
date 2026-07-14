package com.contextengine.security.filesystem;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import com.contextengine.security.isolation.WorkspaceIsolationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Decorator service enforcing symbolic link validation, workspace boundaries, and permissions on filesystem access.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Secure File Access)
 * Responsibility: Prevent symbolic link traversal, verify workspace containment, check disk permissions, and read files securely.
 * Dependencies: {@link FilesystemPort}, {@link WorkspaceIsolationService}, {@link SecurityConstants}, {@link SecurityException}
 * Future Usage: Replaces direct FilesystemPort dependencies in scanner and indexing use cases.
 * </p>
 */
public class SecureFilesystemService {

    private final FilesystemPort filesystemPort;
    private final WorkspaceIsolationService isolationService;

    /**
     * Constructs a SecureFilesystemService.
     *
     * @param filesystemPort the physical filesystem adapter port
     * @param isolationService service validating workspace boundaries
     */
    public SecureFilesystemService(
        FilesystemPort filesystemPort,
        WorkspaceIsolationService isolationService
    ) {
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
        this.isolationService = Objects.requireNonNull(isolationService, "WorkspaceIsolationService must not be null");
    }

    /**
     * Reads the content of a file securely after executing boundary, permission, and symlink validation checks.
     *
     * @param projectId the target project ID context
     * @param filePath the file path to read
     * @return the string contents of the file
     * @throws SecurityException if access is denied, path traversal is detected, or the file is a symbolic link
     */
    public String readSecureFile(ProjectId projectId, com.contextengine.domain.valueobject.Path filePath) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(filePath, "File path must not be null");

        // 1. Enforce symbolic link boundary checks
        java.nio.file.Path path = Paths.get(filePath.value());
        if (Files.isSymbolicLink(path)) {
            throw new SecurityException(
                SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                "Access blocked: Symbolic link traversal is forbidden: " + filePath.value()
            );
        }

        // 2. Validate workspace containment
        isolationService.validateAccess(projectId, filePath);

        // 3. Verify file existence and read permissions
        if (!filesystemPort.exists(filePath)) {
            throw new SecurityException(
                SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                "Access blocked: File does not exist on disk: " + filePath.value()
            );
        }

        if (!filesystemPort.hasReadWritePermissions(filePath) && !Files.isReadable(path)) {
            throw new SecurityException(
                SecurityConstants.ERROR_BOUNDARY_VIOLATION,
                "Access blocked: Insufficient read permissions for file: " + filePath.value()
            );
        }

        // 4. Delegate to the port for physical read operations
        return filesystemPort.readFile(filePath);
    }
}
