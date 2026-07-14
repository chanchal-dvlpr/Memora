package com.contextengine.api.controller;

import com.contextengine.api.ApiPaths;
import com.contextengine.api.request.RegisterProjectRequest;
import com.contextengine.api.response.ProjectResponse;
import com.contextengine.api.service.ProjectRestService;
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

import java.util.List;
import java.util.Objects;

/**
 * REST controller managing project registrations, updates, and list/retrieve operations.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.1 Logical Resource: Project Registry Engine
 * </p>
 */
@RestController
@Tag(name = "Project Management", description = "Endpoints for registering repositories, listing active workspaces, and managing codebases.")
public class ProjectController {

    private final ProjectRestService projectRestService;

    /**
     * Constructs a ProjectController.
     *
     * @param projectRestService the project REST service layer dependency
     */
    public ProjectController(ProjectRestService projectRestService) {
        this.projectRestService = Objects.requireNonNull(projectRestService, "ProjectRestService must not be null");
    }

    /**
     * Registers a new project directory within the platform.
     *
     * @param request the registration details request body
     * @return the registered project response details
     */
    @PostMapping(ApiPaths.PROJECTS)
    @Operation(summary = "Register a new project directory", description = "Sets up tracking for codebase repository root path.")
    @ApiResponse(responseCode = "200", description = "Project registered successfully.")
    @ApiResponse(responseCode = "400", description = "Malformed registration parameters or missing field.")
    @ApiResponse(responseCode = "422", description = "Target directory path overlaps with an existing registered project.")
    public ResponseEntity<ProjectResponse> registerProject(@jakarta.validation.Valid @RequestBody RegisterProjectRequest request) {
        return ResponseEntity.ok(projectRestService.registerProject(request));
    }

    /**
     * Fetches metadata for an existing registered project.
     *
     * @param id the project unique identifier
     * @return the project details response
     */
    @GetMapping(ApiPaths.PROJECTS_ID)
    @Operation(summary = "Retrieve project metadata by ID", description = "Retrieves core tracking configuration and root path for the specified project.")
    @ApiResponse(responseCode = "200", description = "Project details retrieved successfully.")
    @ApiResponse(responseCode = "404", description = "No active project matches the provided UUID.")
    public ResponseEntity<ProjectResponse> getProject(
        @Parameter(description = "The UUID of the registered project") @PathVariable("id") String id
    ) {
        return ResponseEntity.ok(projectRestService.getProject(id));
    }

    /**
     * Lists all registered projects currently active in the index.
     *
     * @return list of project details responses
     */
    @GetMapping(ApiPaths.PROJECTS)
    @Operation(summary = "List active projects", description = "Returns details of all codebases registered with the server.")
    @ApiResponse(responseCode = "200", description = "List of active projects retrieved successfully.")
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        return ResponseEntity.ok(projectRestService.listProjects());
    }

    /**
     * Deregisters a project and schedules cleanups.
     *
     * @param id the project unique identifier
     * @return empty ok response
     */
    @DeleteMapping(ApiPaths.PROJECTS_ID)
    @Operation(summary = "Deregister project", description = "Removes project mapping details and deletes indexed files index from storage.")
    @ApiResponse(responseCode = "200", description = "Project unregistration initiated successfully.")
    @ApiResponse(responseCode = "404", description = "No project found with specified ID.")
    public ResponseEntity<Void> removeProject(
        @Parameter(description = "The UUID of the registered project") @PathVariable("id") String id
    ) {
        projectRestService.removeProject(id);
        return ResponseEntity.ok().build();
    }
}
