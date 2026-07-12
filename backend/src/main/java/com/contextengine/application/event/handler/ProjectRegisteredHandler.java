package com.contextengine.application.event.handler;

import com.contextengine.application.command.ScanProjectCommand;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;
import java.util.UUID;

/**
 * Event handler coordinating operations triggered when a new codebase project is registered.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class ProjectRegisteredHandler {

    private final ProjectApplicationService projectService;

    /**
     * Constructs a ProjectRegisteredHandler.
     *
     * @param projectService project application service coordinator
     */
    public ProjectRegisteredHandler(ProjectApplicationService projectService) {
        this.projectService = Objects.requireNonNull(projectService, "ProjectApplicationService must not be null");
    }

    /**
     * Coordinates the workflow following project registration, triggering an asynchronous workspace scan.
     *
     * @param envelope the event envelope
     */
    public void handle(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        String projectIdStr = (String) envelope.payload().get("projectId");
        if (projectIdStr != null) {
            ProjectId projectId = new ProjectId(UUID.fromString(projectIdStr));
            projectService.scanProject(new ScanProjectCommand(projectId, false, false));
        }
    }
}
