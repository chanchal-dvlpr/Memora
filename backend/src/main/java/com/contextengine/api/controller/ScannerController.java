package com.contextengine.api.controller;

import com.contextengine.api.ApiPaths;
import com.contextengine.api.request.ScanProjectRequest;
import com.contextengine.api.response.ScanStatusResponse;
import com.contextengine.api.service.ScannerRestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST controller coordinating manual codebase scanning traversals, progress metrics, and monitoring.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.3 Logical Resource: Codebase Index Engine
 * </p>
 */
@RestController
@Tag(name = "Index Scanning", description = "Endpoints for triggering manual scans, tracking crawler progress, and monitoring index synchronization status.")
public class ScannerController {

    private final ScannerRestService scannerRestService;

    /**
     * Constructs a ScannerController.
     *
     * @param scannerRestService the scanner REST service layer dependency
     */
    public ScannerController(ScannerRestService scannerRestService) {
        this.scannerRestService = Objects.requireNonNull(scannerRestService, "ScannerRestService must not be null");
    }

    /**
     * Triggers a manual filesystem and AST scan traversal over the project workspace.
     *
     * @param id the project unique identifier
     * @param request the scan depth configuration parameters
     * @return the scan job completion status details
     */
    @PostMapping(ApiPaths.SCANNERS)
    @Operation(summary = "Trigger a manual project scan", description = "Crawls files, extracts abstract syntax trees, resolves dependencies, and updates the knowledge graph.")
    @ApiResponse(responseCode = "200", description = "Index scan completed successfully.")
    @ApiResponse(responseCode = "404", description = "No project found with specified ID.")
    public ResponseEntity<ScanStatusResponse> triggerScan(
        @Parameter(description = "The UUID of the registered project") @PathVariable("id") String id,
        @jakarta.validation.Valid @RequestBody ScanProjectRequest request
    ) {
        return ResponseEntity.ok(scannerRestService.triggerScan(id, request));
    }

    /**
     * Retrieves current progress tracking metrics for the scanner.
     *
     * @param id the project unique identifier
     * @return the scanner diagnostic status
     */
    @GetMapping(ApiPaths.SCANNERS)
    @Operation(summary = "Get scanner progress status", description = "Retrieves active monitoring logs, crawler thread statuses, and file parsing counts.")
    @ApiResponse(responseCode = "200", description = "Scanner status metrics retrieved successfully.")
    @ApiResponse(responseCode = "404", description = "No active project matches the provided UUID.")
    public ResponseEntity<ScanStatusResponse> getScannerStatus(
        @Parameter(description = "The UUID of the registered project") @PathVariable("id") String id
    ) {
        return ResponseEntity.ok(scannerRestService.getScannerStatus(id));
    }
}
