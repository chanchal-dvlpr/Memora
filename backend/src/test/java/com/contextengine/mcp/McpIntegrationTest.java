package com.contextengine.mcp;

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
import com.contextengine.mcp.transport.StdioTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration and protocol validation tests for the Model Context Protocol (MCP) subsystem.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (MCP Integration)
 * Purpose: Validate handshake, protocol parsing, tools, resources, prompts, sessions, security, and loopback transport checks.
 * Dependencies: Spring Boot Test, MockMvc, H2 Database.
 */
@SpringBootTest(properties = {
    "context-engine.mcp.enabled=true",
    "context-engine.mcp.security-tokens=test-integration-token"
})
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
class McpIntegrationTest {

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
    private StdioTransport stdioTransport;

    @Autowired
    private McpProtocolHandler protocolHandler;

    private String activeSessionId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean sessions and database completely to ensure test isolation
        sessionManager.getActiveSessions().forEach(s -> sessionManager.terminateSession(s.getSessionId()));
        springDataProjectRepository.deleteAll();

        // Establish an active session to test authenticated requests
        McpRequest initReq = new McpRequest("2.0", "initialize", Map.of(
            "clientInfo", Map.of("name", "test-client", "version", "1.2.3"),
            "token", "test-integration-token"
        ), 101);

        MvcResult result = mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sessionId").exists())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        McpResponse resp = objectMapper.readValue(json, McpResponse.class);
        Map<?, ?> resultMap = (Map<?, ?>) resp.getResult();
        activeSessionId = (String) resultMap.get("sessionId");

        // Send notifications/initialized to fully transition the session to ACTIVE
        McpRequest initializedReq = new McpRequest("2.0", "notifications/initialized", Map.of(
            "sessionId", activeSessionId
        ), 102);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initializedReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @AfterEach
    void tearDown() {
        if (activeSessionId != null) {
            sessionManager.terminateSession(activeSessionId);
        }
        stdioTransport.stop();
    }

    // ==========================================
    // 1. MCP HANDSHAKE TESTS
    // ==========================================

    @Test
    void testInitializeSuccess() throws Exception {
        McpRequest initReq = new McpRequest("2.0", "initialize", Map.of(
            "clientInfo", Map.of("name", "client-ok", "version", "1.0"),
            "token", "test-integration-token"
        ), 1);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"))
                .andExpect(jsonPath("$.result.capabilities.resources").exists())
                .andExpect(jsonPath("$.result.capabilities.prompts").exists())
                .andExpect(jsonPath("$.result.capabilities.tools").exists())
                .andExpect(jsonPath("$.result.sessionId").exists());
    }

    @Test
    void testInitializeAuthenticationFailure() throws Exception {
        McpRequest initReq = new McpRequest("2.0", "initialize", Map.of(
            "token", "bad-token"
        ), 1);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(4001))
                .andExpect(jsonPath("$.error.message").value(containsString("Authentication failed")));
    }

    @Test
    void testInitializeMissingTokenFailure() throws Exception {
        McpRequest initReq = new McpRequest("2.0", "initialize", Map.of(), 1);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(4001));
    }

    // ==========================================
    // 2. MCP PROTOCOL PARSING TESTS
    // ==========================================

    @Test
    void testProtocolInvalidMethod() throws Exception {
        McpRequest badReq = new McpRequest("2.0", "invalid_method_xyz", Map.of("sessionId", activeSessionId), 2);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andExpect(jsonPath("$.error.message").value("Method not found: invalid_method_xyz"));
    }

    @Test
    void testProtocolInvalidRequestStructure() throws Exception {
        // missing jsonrpc version
        McpRequest badReq = new McpRequest(null, "tools/list", Map.of("sessionId", activeSessionId), 3);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32600));
    }

    @Test
    void testProtocolInvalidParameters() throws Exception {
        // missing required uri parameter in resources/read
        McpRequest req = new McpRequest("2.0", "resources/read", Map.of("sessionId", activeSessionId), 4);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602));
    }

    // ==========================================
    // 3. MCP TOOLS TESTS
    // ==========================================

    @Test
    void testToolsList() throws Exception {
        McpRequest req = new McpRequest("2.0", "tools/list", Map.of("sessionId", activeSessionId), 5);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools").isArray())
                .andExpect(jsonPath("$.result.tools[0].name").exists());
    }

    @Test
    void testToolsCallRegisterProjectAndUnknownTool() throws Exception {
        // Call context_engine_register_project
        String targetPath = new File(".").getCanonicalPath();
        McpRequest registerReq = new McpRequest("2.0", "tools/call", Map.of(
            "sessionId", activeSessionId,
            "name", "context_engine_register_project",
            "arguments", Map.of("targetPath", targetPath)
        ), 6);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.projectId").exists())
                .andReturn();

        String rawJson = mvcResult.getResponse().getContentAsString();
        McpResponse resp = objectMapper.readValue(rawJson, McpResponse.class);
        Map<?, ?> resultMap = (Map<?, ?>) resp.getResult();
        String createdProjectId = (String) resultMap.get("projectId");

        // Assert project repository state
        Optional<Project> projectOpt = projectRepository.findById(ProjectId.fromString(createdProjectId));
        assertThat(projectOpt).isPresent();
        assertThat(projectOpt.get().title()).isEqualTo("backend");

        // Call scan project
        McpRequest scanReq = new McpRequest("2.0", "tools/call", Map.of(
            "sessionId", activeSessionId,
            "name", "context_engine_scan_project",
            "arguments", Map.of("projectId", createdProjectId)
        ), 7);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true));

        // Call search engineering memory
        McpRequest searchReq = new McpRequest("2.0", "tools/call", Map.of(
            "sessionId", activeSessionId,
            "name", "search_engineering_memory",
            "arguments", Map.of("projectId", createdProjectId, "query_string", "Context")
        ), 8);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.results").isArray());

        // Call unknown tool call
        McpRequest unknownToolReq = new McpRequest("2.0", "tools/call", Map.of(
            "sessionId", activeSessionId,
            "name", "unknown_tool_endpoint",
            "arguments", Map.of()
        ), 9);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unknownToolReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601));
    }

    // ==========================================
    // 4. MCP RESOURCES TESTS
    // ==========================================

    @Test
    void testResourcesListAndRead() throws Exception {
        // Register a project using a unique mock path
        String targetPath = new File(".").getCanonicalPath() + "/mock-resources-" + UUID.randomUUID();
        Project project = new Project(ProjectId.generate(), new Path(targetPath), "Test Integrated Project");
        projectRepository.save(project);

        // List Resources
        McpRequest listReq = new McpRequest("2.0", "resources/list", Map.of("sessionId", activeSessionId), 10);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.resources").isArray());

        // Read Resource workspaces
        String resourceUri = "contextengine://projects/" + project.id().value() + "/workspaces";
        McpRequest readReq = new McpRequest("2.0", "resources/read", Map.of(
            "sessionId", activeSessionId,
            "uri", resourceUri
        ), 11);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(readReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents").isArray())
                .andExpect(jsonPath("$.result.contents[0].uri").value(resourceUri))
                .andExpect(jsonPath("$.result.contents[0].text").exists());

        // Missing resource read
        String missingUri = "contextengine://projects/" + project.id().value() + "/nonexistent_path";
        McpRequest missingReq = new McpRequest("2.0", "resources/read", Map.of(
            "sessionId", activeSessionId,
            "uri", missingUri
        ), 12);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602));
    }

    // ==========================================
    // 5. MCP PROMPTS TESTS
    // ==========================================

    @Test
    void testPromptsListAndGet() throws Exception {
        // Register a project using a unique mock path
        String targetPath = new File(".").getCanonicalPath() + "/mock-prompts-" + UUID.randomUUID();
        Project project = new Project(ProjectId.generate(), new Path(targetPath), "Prompt Test Project");
        projectRepository.save(project);

        // List Prompts
        McpRequest listReq = new McpRequest("2.0", "prompts/list", Map.of("sessionId", activeSessionId), 13);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.prompts").isArray());

        // Get Prompt
        McpRequest getReq = new McpRequest("2.0", "prompts/get", Map.of(
            "sessionId", activeSessionId,
            "name", "review-architecture",
            "arguments", Map.of("projectId", project.id().value().toString())
        ), 14);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(getReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.messages").isArray())
                .andExpect(jsonPath("$.result.messages[0].content.text").exists());
    }

    // ==========================================
    // 6. MCP SESSIONS LIFECYCLE TESTS
    // ==========================================

    @Test
    void testSessionLifecycle() {
        // Creation & lookup verified via active session in setup
        Optional<McpSession> sessionOpt = sessionManager.getSession(activeSessionId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().getState().name()).isEqualTo("ACTIVE");

        // Verify pruning updates activity and keeps active session
        sessionManager.pruneExpiredSessions();
        assertThat(sessionManager.getSession(activeSessionId)).isPresent();

        // Verify termination
        sessionManager.terminateSession(activeSessionId);
        assertThat(sessionManager.getSession(activeSessionId)).isEmpty();
        activeSessionId = null;
    }

    // ==========================================
    // 7. MCP SECURITY ENFORCEMENT TESTS
    // ==========================================

    @Test
    void testArchivedProjectRejection() throws Exception {
        // Register a project
        String targetPath = new File(".").getCanonicalPath() + "/mock-archive-" + UUID.randomUUID();
        Project project = new Project(ProjectId.generate(), new Path(targetPath), "Archived Test Project");
        project.archive();
        projectRepository.save(project);

        // Try reading resource workspaces for archived project
        String resourceUri = "contextengine://projects/" + project.id().value() + "/workspaces";
        McpRequest readReq = new McpRequest("2.0", "resources/read", Map.of(
            "sessionId", activeSessionId,
            "uri", resourceUri
        ), 15);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(readReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32001))
                .andExpect(jsonPath("$.error.message").value(containsString("Project is archived")));
    }

    // ==========================================
    // 8. MCP TRANSPORT BOUNDARY TESTS
    // ==========================================

    @Test
    void testHttpTransportLoopbackForbidden() throws Exception {
        McpRequest req = new McpRequest("2.0", "tools/list", Map.of("sessionId", activeSessionId), 16);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(request -> {
                    // Non-loopback external IP address
                    request.setRemoteAddr("192.168.1.100");
                    return request;
                }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(-32001))
                .andExpect(jsonPath("$.error.message").value(containsString("Local Sovereign Security boundary breach")));
    }

    @Test
    void testStdioTransportMessageFlowAndGracefulShutdown() throws Exception {
        McpRequest listReq = new McpRequest("2.0", "tools/list", Map.of("sessionId", activeSessionId), 17);
        String inputLine = objectMapper.writeValueAsString(listReq) + "\n";

        ByteArrayInputStream testIn = new ByteArrayInputStream(inputLine.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream testOut = new ByteArrayOutputStream();

        StdioTransport testTransport = new StdioTransport(protocolHandler, objectMapper, testIn, testOut);

        try {
            // Start Stdio listener
            testTransport.start();
            Assertions.assertTrue(testTransport.isRunning());

            // Await processing and check printed output contains tools list
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                String printed = testOut.toString(StandardCharsets.UTF_8);
                assertThat(printed).contains("tools");
            });

            // Parse response
            String outputStr = testOut.toString(StandardCharsets.UTF_8).trim();
            McpResponse response = objectMapper.readValue(outputStr, McpResponse.class);
            assertThat(response.getJsonrpc()).isEqualTo("2.0");
            assertThat(response.getId()).isEqualTo(17);

            // Shutdown gracefully
            testTransport.stop();
            Assertions.assertFalse(testTransport.isRunning());
        } finally {
            testTransport.stop();
        }
    }
}
