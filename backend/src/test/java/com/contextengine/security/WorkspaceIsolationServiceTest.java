package com.contextengine.security;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityException;
import com.contextengine.security.isolation.WorkspaceBoundaryValidator;
import com.contextengine.security.isolation.WorkspaceIsolationService;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link WorkspaceIsolationService}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Workspace Isolation Service Tests)
 * Purpose: Validate project active state validation and boundary isolation integration.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceIsolationServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceBoundaryValidator boundaryValidator;

    private WorkspaceIsolationService isolationService;

    @BeforeEach
    void setUp() {
        isolationService = new WorkspaceIsolationService(projectRepository, boundaryValidator);
    }

    @Test
    void testValidateAccessSuccess() {
        ProjectId projectId = ProjectId.generate();
        Path rootPath = new Path("/mock/root");
        Path targetPath = new Path("/mock/root/src");

        Project project = new Project(projectId, rootPath, "Mock Project");
        Mockito.when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Assertions.assertDoesNotThrow(() -> {
            isolationService.validateAccess(projectId, targetPath);
        });

        Mockito.verify(boundaryValidator).validateBoundary(
            java.nio.file.Paths.get("/mock/root"),
            java.nio.file.Paths.get("/mock/root/src")
        );
    }

    @Test
    void testValidateAccessProjectNotFoundThrows() {
        ProjectId projectId = ProjectId.generate();
        Path targetPath = new Path("/mock/root/src");

        Mockito.when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            isolationService.validateAccess(projectId, targetPath);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_WORKSPACE_LOCKED, exception.getErrorCode());
        Assertions.assertTrue(exception.getMessage().contains("Project registration not found"));
    }

    @Test
    void testValidateAccessArchivedProjectThrows() {
        ProjectId projectId = ProjectId.generate();
        Path rootPath = new Path("/mock/root");
        Path targetPath = new Path("/mock/root/src");

        Project project = new Project(projectId, rootPath, "Mock Project");
        project.archive();
        Mockito.when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            isolationService.validateAccess(projectId, targetPath);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_WORKSPACE_LOCKED, exception.getErrorCode());
        Assertions.assertTrue(exception.getMessage().contains("Project workspace is archived"));
    }
}
