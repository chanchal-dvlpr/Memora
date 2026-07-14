package com.contextengine.api.service;

import com.contextengine.api.mapper.ProjectResponseMapper;
import com.contextengine.api.request.RegisterProjectRequest;
import com.contextengine.api.response.ProjectResponse;
import com.contextengine.application.command.RegisterProjectCommand;
import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.query.GetProjectQuery;
import com.contextengine.application.query.ListProjectsQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating HTTP presentation concerns for Project registration and lookup.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.1 (Project Registry Engine)
 * </p>
 */
@Service
public class ProjectRestService {

    private final ProjectApplicationService projectService;

    /**
     * Constructs a ProjectRestService.
     *
     * @param projectService application layer project service dependency
     */
    public ProjectRestService(ProjectApplicationService projectService) {
        this.projectService = Objects.requireNonNull(projectService, "ProjectApplicationService must not be null");
    }

    /**
     * Delegates registration requests to application command handlers.
     *
     * @param request project registration request params
     * @return project response payload
     */
    public ProjectResponse registerProject(RegisterProjectRequest request) {
        Objects.requireNonNull(request, "Request must not be null");
        RegisterProjectCommand command = new RegisterProjectCommand(
            new Path(request.getRootPath()),
            request.getName(),
            request.getExclusionPatterns()
        );

        ApplicationResult<ProjectDto> result = projectService.registerProject(command);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Project registration failed"));
        }

        return ProjectResponseMapper.toResponse(result.value().orElseThrow());
    }

    /**
     * Resolves individual project lookup queries.
     *
     * @param id project UUID string
     * @return project response payload
     */
    public ProjectResponse getProject(String id) {
        Objects.requireNonNull(id, "ID must not be null");
        GetProjectQuery query = new GetProjectQuery(new ProjectId(UUID.fromString(id)));
        ApplicationResult<ProjectDto> result = projectService.getProject(query);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Retrieve project failed"));
        }

        return ProjectResponseMapper.toResponse(result.value().orElseThrow());
    }

    /**
     * Resolves project list queries.
     *
     * @return list of active project responses
     */
    public List<ProjectResponse> listProjects() {
        ListProjectsQuery query = new ListProjectsQuery(true);
        ApplicationResult<List<ProjectDto>> result = projectService.listProjects(query);
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("List projects failed"));
        }

        return result.value().orElseThrow().stream()
            .map(ProjectResponseMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Resolves project unregistration commands.
     *
     * @param id project UUID string
     */
    public void removeProject(String id) {
        Objects.requireNonNull(id, "ID must not be null");
        ApplicationResult<Boolean> result = projectService.removeProject(new ProjectId(UUID.fromString(id)));
        if (!result.isSuccess()) {
            throw result.error().orElseThrow(() -> new RuntimeException("Remove project failed"));
        }
    }
}
