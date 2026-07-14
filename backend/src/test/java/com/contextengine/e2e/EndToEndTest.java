package com.contextengine.e2e;

import com.contextengine.api.request.GenerateContextRequest;
import com.contextengine.api.request.RegisterProjectRequest;
import com.contextengine.api.request.ScanProjectRequest;
import com.contextengine.mcp.protocol.McpRequest;
import com.contextengine.mcp.protocol.McpResponse;
import com.contextengine.mcp.session.McpSessionManager;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
    "context-engine.mcp.enabled=true",
    "context-engine.mcp.security-tokens=test-e2e-token"
})
class EndToEndTest extends BaseIntegrationTest {

    @Autowired
    private McpSessionManager sessionManager;

    @TempDir
    java.nio.file.Path tempDir;

    private String activeSessionId;

    @AfterEach
    void cleanSessions() {
        if (activeSessionId != null) {
            sessionManager.terminateSession(activeSessionId);
        }
    }

    @Test
    void testWorkflow1And3_FullRESTLifecycle() throws Exception {
        java.nio.file.Path srcDir = Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(srcDir.resolve("App.java"), "public class App { public void run() {} }");

        String rootPath = tempDir.toRealPath().toString();

        // 1. Register Project
        RegisterProjectRequest registerReq = new RegisterProjectRequest("E2E Project", rootPath, List.of());
        String regResponse = mockMvc.perform(post("/api/v1/projects")
                .header("X-Session-Token", "test-e2e-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(regResponse).get("id").asText();

        // 2. Scan Project
        ScanProjectRequest scanReq = new ScanProjectRequest(true);
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/scanners")
                .header("X-Session-Token", "test-e2e-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanReq)))
                .andExpect(status().isOk());

        // 3. Generate Context
        GenerateContextRequest contextReq = new GenerateContextRequest(projectId, "App", "src/App.java", 2000, "MARKDOWN");
        String contextResponse = mockMvc.perform(post("/api/v1/context/assembly")
                .header("X-Session-Token", "test-e2e-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contextReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").exists())
                .andReturn().getResponse().getContentAsString();

        String snapshotId = objectMapper.readTree(contextResponse).get("contextId").asText();

        // 4. Retrieve Snapshot
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/snapshots/latest")
                .header("X-Session-Token", "test-e2e-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").value(snapshotId));

        // 5. Clean up/Delete Project
        mockMvc.perform(delete("/api/v1/projects/" + projectId)
                .header("X-Session-Token", "test-e2e-token"))
                .andExpect(status().isOk());
    }

    @Test
    void testWorkflow2_IncrementalUpdates() throws Exception {
        java.nio.file.Path srcDir = Files.createDirectories(tempDir.resolve("src"));
        java.nio.file.Path appFile = srcDir.resolve("App.java");
        Files.writeString(appFile, "public class App { public void run() {} }");

        String rootPath = tempDir.toRealPath().toString();

        // Register Project
        RegisterProjectRequest registerReq = new RegisterProjectRequest("E2E Incremental Project", rootPath, List.of());
        String regResponse = mockMvc.perform(post("/api/v1/projects")
                .header("X-Session-Token", "test-e2e-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(regResponse).get("id").asText();

        // First full scan
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/scanners")
                .header("X-Session-Token", "test-e2e-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ScanProjectRequest(true))))
                .andExpect(status().isOk());

        // Check scanner filesProcessed count
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/scanners")
                .header("X-Session-Token", "test-e2e-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesProcessed").value(1));

        // Modify files (Add new Helper.java)
        java.nio.file.Path helperFile = srcDir.resolve("Helper.java");
        Files.writeString(helperFile, "public class Helper { public void help() {} }");

        // Trigger incremental scan
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/scanners")
                .header("X-Session-Token", "test-e2e-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ScanProjectRequest(false))))
                .andExpect(status().isOk());

        // Scanner should process incremental update
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/scanners")
                .header("X-Session-Token", "test-e2e-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesProcessed").value(2));
    }

    @Test
    void testWorkflow4_MCPClientLifecycle() throws Exception {
        // 1. Handshake Initialize
        McpRequest initReq = new McpRequest("2.0", "initialize", Map.of(
            "clientInfo", Map.of("name", "e2e-mcp-client", "version", "1.0.0"),
            "token", "test-e2e-token"
        ), 201);

        MvcResult initResult = mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sessionId").exists())
                .andReturn();

        String json = initResult.getResponse().getContentAsString();
        McpResponse resp = objectMapper.readValue(json, McpResponse.class);
        Map<?, ?> resultMap = (Map<?, ?>) resp.getResult();
        activeSessionId = (String) resultMap.get("sessionId");

        // 2. Initialized Notification
        McpRequest initializedReq = new McpRequest("2.0", "notifications/initialized", Map.of(
            "sessionId", activeSessionId
        ), 202);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initializedReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk());

        // 3. Tool Discovery
        McpRequest toolsReq = new McpRequest("2.0", "tools/list", Map.of("sessionId", activeSessionId), 203);
        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toolsReq))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools").isArray());

        // 4. Register Project Tool Invocation
        String canonicalPath = new File(".").getCanonicalPath();
        McpRequest callRegister = new McpRequest("2.0", "tools/call", Map.of(
            "sessionId", activeSessionId,
            "name", "context_engine_register_project",
            "arguments", Map.of("targetPath", canonicalPath)
        ), 204);

        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callRegister))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.projectId").exists());
    }
}
