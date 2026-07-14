package com.contextengine.api.controller;

import com.contextengine.api.advice.ErrorPayload;
import com.contextengine.api.request.GenerateContextRequest;
import com.contextengine.api.request.RegisterProjectRequest;
import com.contextengine.api.request.ScanProjectRequest;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.dto.ScanStatusDto;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.domain.service.DirectoryAccessDeniedException;
import com.contextengine.domain.service.OverlappingProjectException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level verification tests for all REST API endpoints,
 * including validations, exception mapping outcomes, and API docs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectApplicationService projectService;

    @MockBean
    private ContextApplicationService contextService;

    @Test
    void testRegisterProjectSuccess() throws Exception {
        ProjectDto dto = new ProjectDto("451df677-742a-43d9-95dc-100281b37b60", "/workspace/app", "App Project", List.of());
        Mockito.when(projectService.registerProject(any())).thenReturn(ApplicationResult.success(dto));

        RegisterProjectRequest request = new RegisterProjectRequest("App Project", "/workspace/app", List.of());

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("451df677-742a-43d9-95dc-100281b37b60"))
                .andExpect(jsonPath("$.name").value("App Project"))
                .andExpect(jsonPath("$.rootPath").value("/workspace/app"));
    }

    @Test
    void testRegisterProjectValidationFailure() throws Exception {
        RegisterProjectRequest request = new RegisterProjectRequest("", "", List.of());

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.category").value("VALIDATION"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETERS"))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void testGetProject() throws Exception {
        ProjectDto dto = new ProjectDto("451df677-742a-43d9-95dc-100281b37b60", "/workspace/app", "App Project", List.of());
        Mockito.when(projectService.getProject(any())).thenReturn(ApplicationResult.success(dto));

        mockMvc.perform(get("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("451df677-742a-43d9-95dc-100281b37b60"));
    }

    @Test
    void testListProjects() throws Exception {
        ProjectDto dto = new ProjectDto("451df677-742a-43d9-95dc-100281b37b60", "/workspace/app", "App Project", List.of());
        Mockito.when(projectService.listProjects(any())).thenReturn(ApplicationResult.success(List.of(dto)));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("451df677-742a-43d9-95dc-100281b37b60"));
    }

    @Test
    void testRemoveProject() throws Exception {
        Mockito.when(projectService.removeProject(any())).thenReturn(ApplicationResult.success(true));

        mockMvc.perform(delete("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60"))
                .andExpect(status().isOk());
    }

    @Test
    void testTriggerScan() throws Exception {
        Mockito.when(projectService.scanProject(any())).thenReturn(ApplicationResult.success(true));

        ScanStatusDto statusDto = new ScanStatusDto("451df677-742a-43d9-95dc-100281b37b60", true, 42L, "main", "hash");
        Mockito.when(projectService.getScanStatus(any())).thenReturn(ApplicationResult.success(statusDto));

        ScanProjectRequest request = new ScanProjectRequest(true);

        mockMvc.perform(post("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60/scanners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.filesProcessed").value(42));
    }

    @Test
    void testGetScannerStatus() throws Exception {
        ScanStatusDto statusDto = new ScanStatusDto("451df677-742a-43d9-95dc-100281b37b60", true, 42L, "main", "hash");
        Mockito.when(projectService.getScanStatus(any())).thenReturn(ApplicationResult.success(statusDto));

        mockMvc.perform(get("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60/scanners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("451df677-742a-43d9-95dc-100281b37b60"))
                .andExpect(jsonPath("$.filesProcessed").value(42));
    }

    @Test
    void testCompileContextScopeSuccess() throws Exception {
        ContextSnapshotDto dto = new ContextSnapshotDto("snap-123", "451df677-742a-43d9-95dc-100281b37b60", "payload text", 100, "2026-07-13T12:00:00Z");
        Mockito.when(contextService.generateContext(any())).thenReturn(ApplicationResult.success(dto));

        GenerateContextRequest request = new GenerateContextRequest("451df677-742a-43d9-95dc-100281b37b60", "query text", "App.java", 1000, "MARKDOWN");

        mockMvc.perform(post("/api/v1/context/assembly")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").value("snap-123"))
                .andExpect(jsonPath("$.totalTokensConsumed").value(100))
                .andExpect(jsonPath("$.assembledTextPayload").value("payload text"));
    }

    @Test
    void testCompileContextValidationFailure() throws Exception {
        // Max token budget less than 1000 should trigger a validation error
        GenerateContextRequest request = new GenerateContextRequest("451df677-742a-43d9-95dc-100281b37b60", "query text", "App.java", 500, "MARKDOWN");

        mockMvc.perform(post("/api/v1/context/assembly")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.category").value("VALIDATION"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETERS"));
    }

    @Test
    void testCompileContextInvalidUuid() throws Exception {
        // Invalid UUID format for projectId should trigger a validation error
        GenerateContextRequest request = new GenerateContextRequest("invalid-uuid-string", "query text", "App.java", 2000, "MARKDOWN");

        mockMvc.perform(post("/api/v1/context/assembly")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.category").value("VALIDATION"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETERS"))
                .andExpect(jsonPath("$.details[0].field").value("projectId"));
    }

    @Test
    void testGetLatestSnapshot() throws Exception {
        ContextSnapshotDto dto = new ContextSnapshotDto("snap-123", "451df677-742a-43d9-95dc-100281b37b60", "payload text", 100, "2026-07-13T12:00:00Z");
        Mockito.when(contextService.getLatestSnapshot(any())).thenReturn(ApplicationResult.success(dto));

        mockMvc.perform(get("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60/snapshots/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").value("snap-123"))
                .andExpect(jsonPath("$.totalTokensConsumed").value(100));
    }

    @Test
    void testOverlappingProjectExceptionMapping() throws Exception {
        Mockito.when(projectService.registerProject(any()))
                .thenThrow(new OverlappingProjectException("Workspace path already covered by another registered project"));

        RegisterProjectRequest request = new RegisterProjectRequest("App Project", "/workspace/app", List.of());

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("PATH_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("Workspace path already covered by another registered project"));
    }

    @Test
    void testDirectoryAccessDeniedExceptionMapping() throws Exception {
        Mockito.when(projectService.registerProject(any()))
                .thenThrow(new DirectoryAccessDeniedException("Access denied to workspace directory: /workspace/restricted"));

        RegisterProjectRequest request = new RegisterProjectRequest("Restricted Project", "/workspace/restricted", List.of());

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.category").value("SECURITY"))
                .andExpect(jsonPath("$.code").value("DIRECTORY_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied to workspace directory: /workspace/restricted"));
    }

    @Test
    void testProjectNotFoundExceptionMapping() throws Exception {
        Mockito.when(projectService.getProject(any()))
                .thenThrow(new com.contextengine.application.exception.ProjectNotFoundException("Project not found with ID: 451df677-742a-43d9-95dc-100281b37b60"));

        mockMvc.perform(get("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Project not found with ID: 451df677-742a-43d9-95dc-100281b37b60"));
    }

    @Test
    void testSnapshotNotFoundExceptionMapping() throws Exception {
        Mockito.when(contextService.getLatestSnapshot(any()))
                .thenThrow(new com.contextengine.application.exception.SnapshotNotFoundException("No snapshots available for project: 451df677-742a-43d9-95dc-100281b37b60"));

        mockMvc.perform(get("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60/snapshots/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("SNAPSHOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No snapshots available for project: 451df677-742a-43d9-95dc-100281b37b60"));
    }

    @Test
    void testRemoveProjectNotFound() throws Exception {
        Mockito.when(projectService.removeProject(any()))
                .thenThrow(new com.contextengine.application.exception.ProjectNotFoundException("Project not found with ID: 451df677-742a-43d9-95dc-100281b37b60"));

        mockMvc.perform(delete("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void testTriggerScanProjectNotFound() throws Exception {
        Mockito.when(projectService.scanProject(any()))
                .thenThrow(new com.contextengine.application.exception.ProjectNotFoundException("Project not found: 451df677-742a-43d9-95dc-100281b37b60"));

        ScanProjectRequest request = new ScanProjectRequest(true);

        mockMvc.perform(post("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60/scanners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void testTriggerScanProjectArchived() throws Exception {
        Mockito.when(projectService.scanProject(any()))
                .thenThrow(new com.contextengine.application.knowledge.exception.KnowledgeException("Cannot scan archived project: 451df677-742a-43d9-95dc-100281b37b60", "ERR_SCAN_PROJECT_LOCKED"));

        ScanProjectRequest request = new ScanProjectRequest(true);

        mockMvc.perform(post("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60/scanners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("SCAN_PROJECT_LOCKED"));
    }

    @Test
    void testGetScannerStatusProjectNotFound() throws Exception {
        Mockito.when(projectService.getScanStatus(any()))
                .thenThrow(new com.contextengine.application.exception.ProjectNotFoundException("Project not found with ID: 451df677-742a-43d9-95dc-100281b37b60"));

        mockMvc.perform(get("/api/v1/projects/451df677-742a-43d9-95dc-100281b37b60/scanners"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void testCompileContextProjectNotFound() throws Exception {
        Mockito.when(contextService.generateContext(any()))
                .thenThrow(new com.contextengine.application.exception.ProjectNotFoundException("Project not found with ID: 451df677-742a-43d9-95dc-100281b37b60"));

        GenerateContextRequest request = new GenerateContextRequest("451df677-742a-43d9-95dc-100281b37b60", "query text", "App.java", 2000, "MARKDOWN");

        mockMvc.perform(post("/api/v1/context/assembly")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.category").value("INVARIANT_VIOLATION"))
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void testUnexpectedSystemException() throws Exception {
        Mockito.when(projectService.listProjects(any()))
                .thenThrow(new RuntimeException("Database connectivity failure"));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.category").value("SYSTEM"))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Database connectivity failure"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void testOpenApiDocsExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.info.title").value("Context Engine API"));
    }
}
