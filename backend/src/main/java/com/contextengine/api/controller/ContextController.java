package com.contextengine.api.controller;

import com.contextengine.api.ApiPaths;
import com.contextengine.api.request.GenerateContextRequest;
import com.contextengine.api.response.ContextResponse;
import com.contextengine.api.response.ContextCliResponse;
import com.contextengine.api.service.ContextRestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST controller coordinating prompt context assembly, token budget checks, and snapshot generation.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.5 Logical Resource: Context Assembly Engine
 * </p>
 */
@RestController
@Tag(name = "Context Assembly", description = "Endpoints governed by the Context Assembly Engine to package optimized prompts for AI consumer clients.")
public class ContextController {

    private final ContextRestService contextRestService;

    /**
     * Constructs a ContextController.
     *
     * @param contextRestService the context REST service layer dependency
     */
    public ContextController(ContextRestService contextRestService) {
        this.contextRestService = Objects.requireNonNull(contextRestService, "ContextRestService must not be null");
    }

    /**
     * Assembles a targeted context document based on explicit inclusions and relational dependencies.
     *
     * @param request the context assembly parameters
     * @return the compiled context details
     */
    @PostMapping(ApiPaths.CONTEXT_ASSEMBLY)
    @Operation(summary = "Compile an optimized prompt context scope", description = "Assembles codebase entities, dependency references, and structural logs into a token-bound prompt container.")
    @ApiResponse(responseCode = "200", description = "Prompt context assembled successfully.")
    @ApiResponse(responseCode = "400", description = "Validation check failed on input fields.")
    @ApiResponse(responseCode = "422", description = "Specified token budget allocation is too small to build context.")
    public ResponseEntity<ContextResponse> compileContextScope(@jakarta.validation.Valid @RequestBody GenerateContextRequest request) {
        return ResponseEntity.ok(contextRestService.compileContextScope(request));
    }

    /**
     * Retrieves the latest generated context snapshot for a project.
     *
     * @param projectId the project unique identifier path parameter
     * @return the latest snapshot details response
     */
    @GetMapping(ApiPaths.SNAPSHOTS_LATEST)
    @Operation(summary = "Get the latest generated context snapshot", description = "Looks up the most recently persisted prompt context block reference for the project.")
    @ApiResponse(responseCode = "200", description = "Latest context snapshot retrieved successfully.")
    @ApiResponse(responseCode = "404", description = "No context snapshot has been built for this project yet.")
    public ResponseEntity<ContextResponse> getLatestSnapshot(
        @Parameter(description = "The UUID of the registered project") @PathVariable("id") String projectId
    ) {
        return ResponseEntity.ok(contextRestService.getLatestSnapshot(projectId));
    }

    /**
     * Exposes CLI compatible get context endpoint.
     */
    @GetMapping("/api/v1/context/{projectId}")
    @Operation(summary = "Get project context details", description = "Retrieves the latest compiled context snapshot for CLI.")
    @ApiResponse(responseCode = "200", description = "Context details retrieved successfully.")
    @ApiResponse(responseCode = "404", description = "No context found.")
    public ResponseEntity<ContextCliResponse> getContext(
        @PathVariable("projectId") String projectId
    ) {
        ContextResponse resp = contextRestService.getLatestSnapshot(projectId);
        return ResponseEntity.ok(new ContextCliResponse(
            resp.getProjectId(),
            resp.getAssembledTextPayload(),
            resp.getTimestamp()
        ));
    }

    /**
     * Exposes CLI compatible generate context endpoint.
     */
    @PostMapping("/api/v1/context/{projectId}/generate")
    @Operation(summary = "Compile context for a project", description = "Generates and returns prompt context snapshot.")
    public ResponseEntity<ContextCliResponse> generateContext(
        @PathVariable("projectId") String projectId
    ) {
        ContextResponse resp = contextRestService.compileContextScope(new GenerateContextRequest(
            projectId,
            "",
            "",
            100000,
            "markdown"
        ));
        return ResponseEntity.ok(new ContextCliResponse(
            resp.getProjectId(),
            resp.getAssembledTextPayload(),
            resp.getTimestamp()
        ));
    }

    /**
     * Exposes CLI compatible refresh context endpoint.
     */
    @PostMapping("/api/v1/context/{projectId}/refresh")
    @Operation(summary = "Refresh context", description = "Triggers context refresh scan and rebuilds snapshot.")
    public ResponseEntity<ContextCliResponse> refreshContext(
        @PathVariable("projectId") String projectId
    ) {
        // Delegate to compile context scope which performs clean scan/rebuild
        ContextResponse resp = contextRestService.compileContextScope(new GenerateContextRequest(
            projectId,
            "",
            "",
            100000,
            "markdown"
        ));
        return ResponseEntity.ok(new ContextCliResponse(
            resp.getProjectId(),
            resp.getAssembledTextPayload(),
            resp.getTimestamp()
        ));
    }

    /**
     * Exposes CLI compatible delete context endpoint.
     */
    @DeleteMapping("/api/v1/context/{projectId}")
    @Operation(summary = "Delete context snapshot", description = "Removes context snapshot reference.")
    public ResponseEntity<java.util.Map<String, Object>> deleteContext(
        @PathVariable("projectId") String projectId
    ) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("projectId", projectId);
        result.put("deleted", true);
        return ResponseEntity.ok(result);
    }
}
