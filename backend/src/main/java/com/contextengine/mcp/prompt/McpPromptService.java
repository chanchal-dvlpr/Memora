package com.contextengine.mcp.prompt;

import com.contextengine.domain.entity.Bug;
import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Task;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.McpException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service that handles Model Context Protocol (MCP) Prompt operations.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Model Context Protocol Specification
 * Responsibility: List all discoverable prompts and dynamically populate templates (analyze-bug, review-architecture, prepare-handoff).
 * Dependencies: {@link ProjectRepository}
 * Future Usage: Bound to the MCP protocol handler for servicing prompts/list and prompts/get JSON-RPC packets.
 */
public class McpPromptService {

    private final ProjectRepository projectRepository;

    /**
     * Constructs a McpPromptService.
     *
     * @param projectRepository project repository
     */
    public McpPromptService(ProjectRepository projectRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
    }

    /**
     * Lists all exposed prompt configurations.
     *
     * @return a list of prompts with their arguments descriptor
     */
    public List<McpPrompt> listPrompts() {
        List<McpPrompt> list = new ArrayList<>();

        // analyze-bug
        list.add(new McpPrompt(
            "analyze-bug",
            "Guides the model through a detailed analysis of a registered bug ticket.",
            List.of(
                new McpPromptArgument("projectId", "The unique ID of the project containing the bug", true),
                new McpPromptArgument("bugId", "The unique ID of the bug ticket to analyze", true)
            )
        ));

        // review-architecture
        list.add(new McpPrompt(
            "review-architecture",
            "Presents architecture modules and ADRs for structural design review.",
            List.of(
                new McpPromptArgument("projectId", "The unique ID of the project to review", true)
            )
        ));

        // prepare-handoff
        list.add(new McpPrompt(
            "prepare-handoff",
            "Compiles active task details and milestones into a developer handoff summary.",
            List.of(
                new McpPromptArgument("projectId", "The unique ID of the project", true),
                new McpPromptArgument("featureId", "Optional feature ID filter", false),
                new McpPromptArgument("taskId", "Optional task ID filter", false)
            )
        ));

        return list;
    }

    /**
     * Generates prompt content messages by populating a template.
     *
     * @param name the prompt template name
     * @param arguments input arguments map
     * @return list of chat messages forming the populated prompt
     */
    public List<McpPromptMessage> getPrompt(String name, Map<String, String> arguments) {
        if (name == null) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Prompt name must not be null");
        }
        if (arguments == null || !arguments.containsKey("projectId")) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameter: projectId");
        }

        String projectIdStr = arguments.get("projectId");
        ProjectId projectId = ProjectId.fromString(projectIdStr);
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Project not found for ID: " + projectIdStr));

        switch (name.toLowerCase()) {
            case "analyze-bug":
                return handleAnalyzeBug(project, arguments);
            case "review-architecture":
                return handleReviewArchitecture(project, arguments);
            case "prepare-handoff":
                return handlePrepareHandoff(project, arguments);
            default:
                throw new McpException(McpConstants.ERROR_METHOD_NOT_FOUND, "Unknown prompt: " + name);
        }
    }

    private List<McpPromptMessage> handleAnalyzeBug(Project project, Map<String, String> arguments) {
        String bugIdStr = arguments.get("bugId");
        if (bugIdStr == null || bugIdStr.trim().isEmpty()) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameter: bugId");
        }

        Bug bug = project.bugs().stream()
            .filter(b -> b.id().value().toString().equals(bugIdStr))
            .findFirst()
            .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Bug ticket not found: " + bugIdStr));

        String promptText = String.format(
            "You are an expert software engineer and debugger.\n" +
            "Please analyze the following bug ticket and propose a root cause analysis and structural fix:\n\n" +
            "Project: %s (Root Directory: %s)\n" +
            "Bug ID: %s\n" +
            "Manifest Location: %s\n" +
            "Status: %s\n\n" +
            "Propose a fix strategy adhering strictly to our clean architecture standards.",
            project.title(),
            project.rootDirectory().value(),
            bug.id().value().toString(),
            bug.filePath() != null ? bug.filePath().value() : bug.commitHash(),
            bug.status().toString()
        );

        return List.of(new McpPromptMessage("user", McpPromptContent.text(promptText)));
    }

    private List<McpPromptMessage> handleReviewArchitecture(Project project, Map<String, String> arguments) {
        StringBuilder adrsBuilder = new StringBuilder();
        for (Decision decision : project.decisions()) {
            adrsBuilder.append(String.format("- ADR: %s [%s] (Status: %s)\n",
                decision.title(), decision.id().value().toString(), decision.status().toString()));
        }

        String promptText = String.format(
            "You are an expert Software Architect.\n" +
            "Review the current architecture layout and decision records for the project:\n\n" +
            "Project Title: %s\n" +
            "Total Modules: %d\n" +
            "Architectural Decision Records:\n%s\n" +
            "Please evaluate the design coherence and verify if there are potential dependency cycles or modular violations.",
            project.title(),
            project.modules().size(),
            adrsBuilder.length() > 0 ? adrsBuilder.toString() : "No decision records registered."
        );

        return List.of(new McpPromptMessage("user", McpPromptContent.text(promptText)));
    }

    private List<McpPromptMessage> handlePrepareHandoff(Project project, Map<String, String> arguments) {
        String featureIdFilter = arguments.get("featureId");
        String taskIdFilter = arguments.get("taskId");

        StringBuilder content = new StringBuilder();
        content.append(String.format("Handoff Report for Project: %s\n", project.title()));
        content.append(String.format("Project State: %s\n\n", project.state().toString()));

        if (featureIdFilter != null) {
            Optional<Feature> featureOpt = project.features().stream()
                .filter(f -> f.id().value().toString().equals(featureIdFilter))
                .findFirst();
            if (featureOpt.isPresent()) {
                Feature feature = featureOpt.get();
                content.append(String.format("Focus Milestone Feature: %s\nProgress: %.2f%%\n\n",
                    feature.title(), feature.progressPercentage()));
            }
        }

        content.append("Active Tasks:\n");
        for (Task task : project.tasks()) {
            if (taskIdFilter != null && !task.id().value().toString().equals(taskIdFilter)) {
                continue;
            }
            content.append(String.format("- Task: %s [%s] (Status: %s)\n",
                task.description(), task.id().value().toString(), task.status().toString()));
        }

        String promptText = content.toString() + "\nReview this work snapshot and draft a clean transition email or developer handoff notes.";
        return List.of(new McpPromptMessage("user", McpPromptContent.text(promptText)));
    }
}
