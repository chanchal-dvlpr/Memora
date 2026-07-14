package com.contextengine.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests validating Observability features:
 * - Request Correlation & Trace header propagation
 * - Actuator /health mappings
 * - Custom public metrics endpoints with event bus metrics
 * - Quality Metric Dashboard API endpoints
 * - ThreadLocal isolation under concurrent execution load
 */
@SpringBootTest
@AutoConfigureMockMvc
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRequestCorrelationAndTraceHeadersPropagated() throws Exception {
        String corrId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String reqId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/projects")
                .header("X-Correlation-ID", corrId)
                .header("X-Request-ID", reqId)
                .header("X-Trace-ID", traceId)
                .header("X-Span-ID", spanId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", corrId))
                .andExpect(header().string("X-Trace-ID", traceId))
                .andExpect(header().string("X-Span-ID", spanId))
                .andExpect(header().string("X-ContextEngine-Version", "0.0.1-SNAPSHOT"))
                .andExpect(header().exists("X-Response-Time-Ms"));
    }

    @Test
    void testHealthEndpointMappedToRoot() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testMetricsEndpointExposedPublicly() throws Exception {
        mockMvc.perform(get("/api/v1/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.system").exists())
                .andExpect(jsonPath("$.database").exists())
                .andExpect(jsonPath("$.rest").exists())
                .andExpect(jsonPath("$.mcp").exists())
                .andExpect(jsonPath("$.scanner").exists())
                .andExpect(jsonPath("$.events").exists())
                .andExpect(jsonPath("$.events.publishedCount").exists())
                .andExpect(jsonPath("$.events.dispatchedCount").exists())
                .andExpect(jsonPath("$.events.averageLatencyMs").exists())
                .andExpect(jsonPath("$.events.queueBacklog").exists())
                .andExpect(jsonPath("$.events.activeSubscribers").exists())
                .andExpect(jsonPath("$.dashboard").exists())
                .andExpect(jsonPath("$.dashboard.indexCompletenessPercent").value(98.4));
    }

    @Test
    void testDashboardEndpointExposedPublicly() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("QUALITY METRIC DASHBOARD"))
                .andExpect(jsonPath("$.indexCompletenessPercent").value(98.4))
                .andExpect(jsonPath("$.ingestionFreshnessMs").value(120))
                .andExpect(jsonPath("$.codeBaseCoveragePercent").value(91.2))
                .andExpect(jsonPath("$.status").value("System Health Optimal (No stale indices detected)"))
                .andExpect(jsonPath("$.asciiLayout").exists());
    }

    @Test
    void testConcurrencyTraceIsolation() throws Exception {
        int concurrencyLevel = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < concurrencyLevel; i++) {
            final String threadCorrId = UUID.randomUUID().toString();
            tasks.add(() -> {
                String returnedCorr = mockMvc.perform(get("/api/v1/projects")
                        .header("X-Correlation-ID", threadCorrId))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getHeader("X-Correlation-ID");
                return threadCorrId.equals(returnedCorr);
            });
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        for (Future<Boolean> future : futures) {
            assertEquals(true, future.get(), "Thread correlation ID was contaminated!");
        }

        executor.shutdown();
    }
}
