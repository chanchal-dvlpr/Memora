package com.contextengine.production;

import com.contextengine.mcp.protocol.McpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "context-engine.mcp.enabled=true",
    "context-engine.mcp.security-tokens=prod-token"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Transactional
class ProductionVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProductionContextLoads() {
        // Simple assertion to verify that application context boots up successfully in prod profile
    }

    @Test
    void testProductionActuatorEndpoints() throws Exception {
        // Health endpoint should be UP and not show details
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());

        // Info endpoint should load successfully
        mockMvc.perform(get("/info"))
                .andExpect(status().isOk());
    }

    @Test
    void testProductionSecurityBoundaryEnforced() throws Exception {
        McpRequest req = new McpRequest("2.0", "tools/list", Map.of("token", "prod-token"), 1);
        String requestJson = objectMapper.writeValueAsString(req);

        // Request from non-loopback IP must be rejected with 403 Forbidden
        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(request -> {
                    request.setRemoteAddr("192.168.1.50");
                    return request;
                }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(-32001))
                .andExpect(jsonPath("$.error.message").value(containsString("Local Sovereign Security boundary breach")));

        // Request from loopback IP must not be rejected with Forbidden
        mockMvc.perform(post("/api/v1/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void testProductionErrorDetailsHidden() throws Exception {
        // Calling a nonexistent REST API route should not return detailed stack traces
        mockMvc.perform(get("/api/v1/nonexistent-route")
                .header("X-Session-Token", "prod-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
