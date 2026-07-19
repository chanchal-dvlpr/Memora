package com.contextengine.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testNonExistentRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/invalid-endpoint-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.category").value("SYSTEM"))
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));
    }
}
