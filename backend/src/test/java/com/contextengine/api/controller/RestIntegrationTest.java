package com.contextengine.api.controller;

import com.contextengine.api.request.GenerateContextRequest;
import com.contextengine.api.request.RegisterProjectRequest;
import com.contextengine.api.request.ScanProjectRequest;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import java.nio.file.Files;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end REST Integration Tests verifying workflows through actual controllers,
 * services, ports, and H2 database persistence.
 */
@TestPropertySource(properties = {
    "context-engine.mcp.security-tokens=test-integration-token"
})
class RestIntegrationTest extends BaseIntegrationTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void testCompleteRestWorkflow() throws Exception {
        // Create a Java file in the temp directory so scanner has something to scan
        java.nio.file.Path srcDir = Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(srcDir.resolve("App.java"), "public class App { public void run() {} }");

        String absolutePath = tempDir.toRealPath().toString();

        // 1. Register project
        RegisterProjectRequest registerReq = new RegisterProjectRequest("App Project", absolutePath, List.of());
        String responseContent = mockMvc.perform(post("/api/v1/projects")
                .header("X-Session-Token", "test-integration-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("App Project"))
                .andExpect(jsonPath("$.rootPath").value(absolutePath))
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(responseContent).get("id").asText();

        // 2. Retrieve project
        mockMvc.perform(get("/api/v1/projects/" + projectId)
                .header("X-Session-Token", "test-integration-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));

        // 3. Trigger scanner
        ScanProjectRequest scanReq = new ScanProjectRequest(true);
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/scanners")
                .header("X-Session-Token", "test-integration-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanReq)))
                .andExpect(status().isOk());

        // 4. Retrieve scanner status
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/scanners")
                .header("X-Session-Token", "test-integration-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesProcessed").value(1));

        // 5. Generate context
        GenerateContextRequest contextReq = new GenerateContextRequest(projectId, "App", "src/App.java", 2000, "MARKDOWN");
        mockMvc.perform(post("/api/v1/context/assembly")
                .header("X-Session-Token", "test-integration-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contextReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").exists());

        // 6. Get latest snapshot
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/snapshots/latest")
                .header("X-Session-Token", "test-integration-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").exists());

        // 7. Remove project
        mockMvc.perform(delete("/api/v1/projects/" + projectId)
                .header("X-Session-Token", "test-integration-token"))
                .andExpect(status().isOk());
    }
}
