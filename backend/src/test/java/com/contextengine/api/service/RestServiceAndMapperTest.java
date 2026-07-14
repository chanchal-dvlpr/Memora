package com.contextengine.api.service;

import com.contextengine.api.mapper.ContextResponseMapper;
import com.contextengine.api.mapper.ProjectResponseMapper;
import com.contextengine.api.mapper.ScanResponseMapper;
import com.contextengine.api.request.GenerateContextRequest;
import com.contextengine.api.request.RegisterProjectRequest;
import com.contextengine.api.request.ScanProjectRequest;
import com.contextengine.api.response.ContextResponse;
import com.contextengine.api.response.ProjectResponse;
import com.contextengine.api.response.ScanStatusResponse;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.dto.ScanStatusDto;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.application.service.ProjectApplicationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests verifying behavior of the presentation REST Services and Response Mappers.
 */
class RestServiceAndMapperTest {

    private ProjectApplicationService projectApplicationService;
    private ContextApplicationService contextApplicationService;

    private ProjectRestService projectRestService;
    private ScannerRestService scannerRestService;
    private ContextRestService contextRestService;

    @BeforeEach
    void setUp() {
        projectApplicationService = Mockito.mock(ProjectApplicationService.class);
        contextApplicationService = Mockito.mock(ContextApplicationService.class);

        projectRestService = new ProjectRestService(projectApplicationService);
        scannerRestService = new ScannerRestService(projectApplicationService);
        contextRestService = new ContextRestService(contextApplicationService);
    }

    @Test
    void testProjectResponseMapper() {
        ProjectDto dto = new ProjectDto(
            "451df677-742a-43d9-95dc-100281b37b60",
            "/workspace/test",
            "Test Project",
            List.of("*.log")
        );

        ProjectResponse response = ProjectResponseMapper.toResponse(dto);
        Assertions.assertEquals("451df677-742a-43d9-95dc-100281b37b60", response.getId());
        Assertions.assertEquals("Test Project", response.getName());
        Assertions.assertEquals("/workspace/test", response.getRootPath());
        Assertions.assertTrue(response.getExclusionPatterns().contains("*.log"));
    }

    @Test
    void testScanResponseMapper() {
        ScanStatusDto dto = new ScanStatusDto(
            "451df677-742a-43d9-95dc-100281b37b60",
            true,
            42L,
            "main",
            "hash"
        );

        ScanStatusResponse response = ScanResponseMapper.toResponse(dto);
        Assertions.assertEquals("451df677-742a-43d9-95dc-100281b37b60", response.getProjectId());
        Assertions.assertEquals("COMPLETED", response.getStatus());
        Assertions.assertEquals(42L, response.getFilesProcessed());
    }

    @Test
    void testContextResponseMapper() {
        ContextSnapshotDto dto = new ContextSnapshotDto(
            "snap-123",
            "451df677-742a-43d9-95dc-100281b37b60",
            "assembled payload text",
            500,
            "2026-07-13T12:00:00Z"
        );

        ContextResponse response = ContextResponseMapper.toResponse(dto);
        Assertions.assertEquals("snap-123", response.getContextId());
        Assertions.assertEquals("451df677-742a-43d9-95dc-100281b37b60", response.getProjectId());
        Assertions.assertEquals(500, response.getTotalTokensConsumed());
        Assertions.assertEquals("assembled payload text", response.getAssembledTextPayload());
    }

    @Test
    void testProjectRestServiceDelegation() {
        ProjectDto dto = new ProjectDto(
            "451df677-742a-43d9-95dc-100281b37b60",
            "/workspace/test",
            "Test Project",
            List.of()
        );

        Mockito.when(projectApplicationService.registerProject(any()))
            .thenReturn(ApplicationResult.success(dto));

        RegisterProjectRequest request = new RegisterProjectRequest("Test Project", "/workspace/test", List.of());
        ProjectResponse response = projectRestService.registerProject(request);

        Assertions.assertEquals("Test Project", response.getName());
        Mockito.verify(projectApplicationService).registerProject(any());
    }

    @Test
    void testScannerRestServiceDelegation() {
        ScanStatusDto dto = new ScanStatusDto(
            "451df677-742a-43d9-95dc-100281b37b60",
            true,
            12L,
            "main",
            "hash"
        );

        Mockito.when(projectApplicationService.scanProject(any()))
            .thenReturn(ApplicationResult.success(true));
        Mockito.when(projectApplicationService.getScanStatus(any()))
            .thenReturn(ApplicationResult.success(dto));

        ScanProjectRequest request = new ScanProjectRequest(true);
        ScanStatusResponse response = scannerRestService.triggerScan("451df677-742a-43d9-95dc-100281b37b60", request);

        Assertions.assertEquals("COMPLETED", response.getStatus());
        Assertions.assertEquals(12L, response.getFilesProcessed());
    }

    @Test
    void testContextRestServiceDelegation() {
        ContextSnapshotDto dto = new ContextSnapshotDto(
            "snap-123",
            "451df677-742a-43d9-95dc-100281b37b60",
            "assembled payload text",
            500,
            "2026-07-13T12:00:00Z"
        );

        Mockito.when(contextApplicationService.generateContext(any()))
            .thenReturn(ApplicationResult.success(dto));

        GenerateContextRequest request = new GenerateContextRequest("451df677-742a-43d9-95dc-100281b37b60", "test query", "App.java", 2000, "MARKDOWN");
        ContextResponse response = contextRestService.compileContextScope(request);

        Assertions.assertEquals("snap-123", response.getContextId());
        Assertions.assertEquals(500, response.getTotalTokensConsumed());
        Mockito.verify(contextApplicationService).generateContext(any());
    }
}
