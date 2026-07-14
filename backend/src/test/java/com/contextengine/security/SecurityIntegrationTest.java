package com.contextengine.security;

import com.contextengine.api.request.RegisterProjectRequest;
import com.contextengine.application.port.FilesystemPort;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.protocol.McpErrorResponse;
import com.contextengine.mcp.protocol.McpProtocolHandler;
import com.contextengine.mcp.protocol.McpRequest;
import com.contextengine.mcp.protocol.McpResponse;
import com.contextengine.mcp.session.McpSession;
import com.contextengine.mcp.session.McpSessionManager;
import com.contextengine.security.authorization.AuthorizationService;
import com.contextengine.security.filesystem.SecureFilesystemService;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityContext;
import com.contextengine.security.foundation.SecurityException;
import com.contextengine.security.isolation.WorkspaceIsolationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration tests for Context Engine Security Subsystem.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Security Integration Tests)
 * Purpose: Verify authentication, authorization, workspace isolation, secure file access,
 * REST security, MCP security, SecurityContext lifecycle, and prevent regression.
 * </p>
 */
@SpringBootTest(properties = {
    "context-engine.mcp.enabled=true",
    "context-engine.mcp.security-tokens=test-token-1,test-token-2"
})
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private com.contextengine.persistence.repository.SpringDataProjectRepository springDataProjectRepository;

    @Autowired
    private McpSessionManager sessionManager;

    @Autowired
    private McpProtocolHandler mcpProtocolHandler;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private WorkspaceIsolationService workspaceIsolationService;

    @Autowired
    private SecureFilesystemService secureFilesystemService;

    @MockitoBean
    private FilesystemPort filesystemPort;

    private java.nio.file.Path tempDir;
    private java.nio.file.Path targetFile;
    private java.nio.file.Path symlinkFile;

    @BeforeEach
    void setUp() throws IOException {
        sessionManager.getActiveSessions().forEach(s -> sessionManager.terminateSession(s.getSessionId()));
        springDataProjectRepository.deleteAll();

        Mockito.when(filesystemPort.exists(Mockito.any())).thenReturn(true);
        Mockito.when(filesystemPort.hasReadWritePermissions(Mockito.any())).thenReturn(true);

        // Create realistic temp files for secure file testing
        tempDir = Files.createTempDirectory("sec-integration");
        targetFile = Files.createFile(tempDir.resolve("target.txt"));
        try {
            symlinkFile = Files.createSymbolicLink(tempDir.resolve("link.txt"), targetFile);
        } catch (UnsupportedOperationException | IOException e) {
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
        SecurityContext.clear();
    }

    // ==========================================
    // 1. Authentication Tests
    // ==========================================

    @Test
    void testAuthenticationRESTSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                .header("X-Session-Token", "test-token-1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects")
                .header("Authorization", "Bearer test-token-2"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthenticationRESTFailure() throws Exception {
        // Invalid token
        mockMvc.perform(get("/api/v1/projects")
                .header("X-Session-Token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.category").value("SECURITY"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // Missing credentials
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());

        // Malformed credentials
        mockMvc.perform(get("/api/v1/projects")
                .header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. Authorization Tests
    // ==========================================

    @Test
    void testAuthorizationRESTGETvsPOST() throws Exception {
        // GET request is authorized for read operation
        mockMvc.perform(get("/api/v1/projects")
                .header("X-Session-Token", "test-token-1"))
                .andExpect(status().isOk());

        // POST request requires write permission but is authenticated correctly
        RegisterProjectRequest request = new RegisterProjectRequest("test-project", tempDir.toAbsolutePath().toString(), Collections.emptyList());
        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Session-Token", "test-token-1"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthorizationScopeMissingThrows() {
        // Principal bound with wrong scope
        SecurityContext.setPrincipal(new SecurityContext.Principal("caller", Set.of("mcp:read")));

        // Calling checkPermission for non-existent scope should throw
        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            authorizationService.checkPermission("mcp:write_graph");
        });
        Assertions.assertEquals(SecurityConstants.ERROR_SCOPE_INSUFFICIENT, exception.getErrorCode());
    }

    // ==========================================
    // 3. Workspace Isolation Tests
    // ==========================================

    @Test
    void testWorkspaceIsolationAccessAsserts() {
        ProjectId pId = ProjectId.generate();
        Project activeProject = new Project(pId, new Path(tempDir.toAbsolutePath().toString()), "Active Project");
        activeProject.activate();
        projectRepository.save(activeProject);

        // Path inside root
        Path insidePath = new Path(targetFile.toAbsolutePath().toString());
        Assertions.assertDoesNotThrow(() -> workspaceIsolationService.validateAccess(pId, insidePath));

        // Path escaping root via directory traversal
        Path traversalPath = new Path("/tmp/escaped-path/outside.txt");
        SecurityException exception1 = Assertions.assertThrows(SecurityException.class, () -> {
            workspaceIsolationService.validateAccess(pId, traversalPath);
        });
        Assertions.assertEquals(SecurityConstants.ERROR_BOUNDARY_VIOLATION, exception1.getErrorCode());

        // Archived Project
        ProjectId archivedProjId = ProjectId.generate();
        Project archivedProject = new Project(archivedProjId, new Path(tempDir.toAbsolutePath().toString()), "Archived Project");
        archivedProject.archive();
        projectRepository.save(archivedProject);
        SecurityException exception2 = Assertions.assertThrows(SecurityException.class, () -> {
            workspaceIsolationService.validateAccess(archivedProjId, insidePath);
        });
        Assertions.assertEquals(SecurityConstants.ERROR_WORKSPACE_LOCKED, exception2.getErrorCode());

        // Unregistered Project ID
        SecurityException exception3 = Assertions.assertThrows(SecurityException.class, () -> {
            workspaceIsolationService.validateAccess(ProjectId.generate(), insidePath);
        });
        Assertions.assertEquals(SecurityConstants.ERROR_WORKSPACE_LOCKED, exception3.getErrorCode());
    }

    // ==========================================
    // 4. Secure File Access Tests
    // ==========================================

    @Test
    void testSecureFilesystemAccess() {
        ProjectId pId = ProjectId.generate();
        Project project = new Project(pId, new Path(tempDir.toAbsolutePath().toString()), "Test Project");
        project.activate();
        projectRepository.save(project);

        Path insidePath = new Path(targetFile.toAbsolutePath().toString());
        Mockito.when(filesystemPort.exists(insidePath)).thenReturn(true);
        Mockito.when(filesystemPort.hasReadWritePermissions(insidePath)).thenReturn(true);
        Mockito.when(filesystemPort.readFile(insidePath)).thenReturn("secure-content");

        String content = secureFilesystemService.readSecureFile(pId, insidePath);
        Assertions.assertEquals("secure-content", content);

        // Blocked symbolic link access
        if (symlinkFile != null) {
            Path linkPath = new Path(symlinkFile.toAbsolutePath().toString());
            SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
                secureFilesystemService.readSecureFile(pId, linkPath);
            });
            Assertions.assertEquals(SecurityConstants.ERROR_BOUNDARY_VIOLATION, exception.getErrorCode());
            Assertions.assertTrue(exception.getMessage().contains("Symbolic link traversal is forbidden"));
        }
    }

    // ==========================================
    // 5. REST Security & GlobalExceptionHandler
    // ==========================================

    @Test
    void testRESTSecurityMapping() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                .header("X-Session-Token", "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.category").value("SECURITY"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(containsString("Authentication failed")));
    }

    // ==========================================
    // 6. MCP Security Tests
    // ==========================================

    @Test
    void testMCPSecurityValidation() throws Exception {
        // Initialize MCP Session
        McpRequest initReq = new McpRequest("2.0", "initialize", Map.of(
            "clientInfo", Map.of("name", "test-mcp-client", "version", "1.0.0"),
            "token", "test-token-1"
        ), 1);

        MvcResult initResult = mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sessionId").exists())
                .andReturn();

        String json = initResult.getResponse().getContentAsString();
        McpResponse resp = objectMapper.readValue(json, McpResponse.class);
        Map<?, ?> resultMap = (Map<?, ?>) resp.getResult();
        String sessionId = (String) resultMap.get("sessionId");

        // Send notifications/initialized
        McpRequest initializedReq = new McpRequest("2.0", "notifications/initialized", Map.of(
            "sessionId", sessionId
        ), 2);
        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initializedReq)))
                .andExpect(status().isOk());

        // Perform tool check with session
        McpRequest toolsListReq = new McpRequest("2.0", "tools/list", Map.of(
            "sessionId", sessionId
        ), 3);
        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toolsListReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools").exists());

        // Call resource list with invalid token/unauthenticated session list
        McpRequest unauthenticatedReq = new McpRequest("2.0", "resources/list", Map.of(
            "sessionId", "non-existent-session-id"
        ), 4);
        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unauthenticatedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(4001));
    }

    // ==========================================
    // 7. SecurityContext Lifecycle Tests
    // ==========================================

    @Test
    void testSecurityContextLifecycleRESTAndMCP() throws Exception {
        // Assert empty initially
        Assertions.assertFalse(SecurityContext.getPrincipal().isPresent());

        // Execute successful REST call
        mockMvc.perform(get("/api/v1/projects")
                .header("X-Session-Token", "test-token-1"))
                .andExpect(status().isOk());

        // Context must be cleared post-request execution
        Assertions.assertFalse(SecurityContext.getPrincipal().isPresent());

        // Execute abortive REST call (invalid token)
        mockMvc.perform(get("/api/v1/projects")
                .header("X-Session-Token", "invalid-token"))
                .andExpect(status().isUnauthorized());

        // Context must still be cleared
        Assertions.assertFalse(SecurityContext.getPrincipal().isPresent());

        // Directly call McpProtocolHandler
        McpRequest toolsReq = new McpRequest("2.0", "tools/list", Collections.emptyMap(), 10);
        mcpProtocolHandler.handleRequest(toolsReq);

        // Context must be cleared post-MCP execution
        Assertions.assertFalse(SecurityContext.getPrincipal().isPresent());
    }

    // ==========================================
    // 8. Regression Tests
    // ==========================================

    @Test
    void testRegressionExistingModulesWorking() {
        // Check that project repos and standard dependency structures compile and function normally
        ProjectId id = ProjectId.generate();
        Project activeProject = new Project(id, new Path("/tmp/reg-proj"), "Regression Project");
        activeProject.activate();
        projectRepository.save(activeProject);
        assertThat(projectRepository.findById(id)).isPresent();
    }
}
