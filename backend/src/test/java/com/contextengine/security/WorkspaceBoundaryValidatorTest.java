package com.contextengine.security;

import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import com.contextengine.security.isolation.WorkspaceBoundaryValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WorkspaceBoundaryValidator}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Workspace Isolation Boundary Tests)
 * Purpose: Validate path canonicalization, containment detection, and directory traversal blocks.
 * </p>
 */
class WorkspaceBoundaryValidatorTest {

    private WorkspaceBoundaryValidator validator;
    private Path tempRoot;
    private Path insideDir;
    private Path outsideDir;

    @BeforeEach
    void setUp() throws IOException {
        validator = new WorkspaceBoundaryValidator();
        tempRoot = Files.createTempDirectory("ce-test-root");
        insideDir = Files.createDirectory(tempRoot.resolve("inside"));
        outsideDir = Files.createTempDirectory("ce-test-outside");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(insideDir.resolve("test.txt"));
        Files.deleteIfExists(insideDir);
        Files.deleteIfExists(tempRoot);
        Files.deleteIfExists(outsideDir.resolve("test.txt"));
        Files.deleteIfExists(outsideDir);
    }

    @Test
    void testValidateBoundarySuccessForInsideFile() throws IOException {
        Path file = Files.createFile(insideDir.resolve("test.txt"));

        Assertions.assertDoesNotThrow(() -> {
            validator.validateBoundary(tempRoot, file);
        });
    }

    @Test
    void testValidateBoundarySuccessForExactRoot() {
        Assertions.assertDoesNotThrow(() -> {
            validator.validateBoundary(tempRoot, tempRoot);
        });
    }

    @Test
    void testValidateBoundaryOutsideThrows() throws IOException {
        Path file = Files.createFile(outsideDir.resolve("test.txt"));

        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            validator.validateBoundary(tempRoot, file);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_BOUNDARY_VIOLATION, exception.getErrorCode());
    }

    @Test
    void testValidateBoundaryDirectoryTraversalThrows() {
        Path traversalPath = tempRoot.resolve("../").resolve(outsideDir.getFileName());

        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            validator.validateBoundary(tempRoot, traversalPath);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_BOUNDARY_VIOLATION, exception.getErrorCode());
    }
}
