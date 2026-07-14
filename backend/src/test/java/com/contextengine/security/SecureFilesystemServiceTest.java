package com.contextengine.security;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import com.contextengine.security.filesystem.SecureFilesystemService;
import com.contextengine.security.isolation.WorkspaceIsolationService;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SecureFilesystemService}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Secure Filesystem Access Tests)
 * Purpose: Verify read boundary checks, symbolic link blocking, and permission checks.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SecureFilesystemServiceTest {

    @Mock
    private FilesystemPort filesystemPort;

    @Mock
    private WorkspaceIsolationService isolationService;

    private SecureFilesystemService secureFilesystemService;
    private java.nio.file.Path tempDir;
    private java.nio.file.Path targetFile;
    private java.nio.file.Path symlinkFile;

    @BeforeEach
    void setUp() throws IOException {
        secureFilesystemService = new SecureFilesystemService(filesystemPort, isolationService);
        tempDir = Files.createTempDirectory("secure-fs-test");
        targetFile = Files.createFile(tempDir.resolve("target.txt"));
        try {
            symlinkFile = Files.createSymbolicLink(tempDir.resolve("link.txt"), targetFile);
        } catch (UnsupportedOperationException | IOException e) {
            // Fallback for filesystems that do not support symbolic links during tests
            symlinkFile = null;
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (symlinkFile != null) {
            Files.deleteIfExists(symlinkFile);
        }
        Files.deleteIfExists(targetFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testReadSecureFileSuccess() {
        ProjectId projectId = ProjectId.generate();
        Path filePath = new Path(targetFile.toAbsolutePath().toString());

        Mockito.when(filesystemPort.exists(filePath)).thenReturn(true);
        Mockito.when(filesystemPort.hasReadWritePermissions(filePath)).thenReturn(true);
        Mockito.when(filesystemPort.readFile(filePath)).thenReturn("secure-file-content");

        String result = secureFilesystemService.readSecureFile(projectId, filePath);

        Assertions.assertEquals("secure-file-content", result);
        Mockito.verify(isolationService).validateAccess(projectId, filePath);
    }

    @Test
    void testReadSecureFileSymbolicLinkThrows() {
        if (symlinkFile == null) {
            return; // Skip if symbolic links are unsupported by host OS/filesystem
        }

        ProjectId projectId = ProjectId.generate();
        Path filePath = new Path(symlinkFile.toAbsolutePath().toString());

        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            secureFilesystemService.readSecureFile(projectId, filePath);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_BOUNDARY_VIOLATION, exception.getErrorCode());
        Assertions.assertTrue(exception.getMessage().contains("Symbolic link traversal is forbidden"));
    }

    @Test
    void testReadSecureFileMissingFileThrows() {
        ProjectId projectId = ProjectId.generate();
        Path filePath = new Path("/mock/nonexistent.txt");

        Mockito.when(filesystemPort.exists(filePath)).thenReturn(false);

        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            secureFilesystemService.readSecureFile(projectId, filePath);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_BOUNDARY_VIOLATION, exception.getErrorCode());
        Assertions.assertTrue(exception.getMessage().contains("File does not exist"));
    }
}
