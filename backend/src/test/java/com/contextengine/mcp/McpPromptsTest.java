package com.contextengine.mcp;

import com.contextengine.domain.entity.Bug;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.BugId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.mcp.prompt.McpPrompt;
import com.contextengine.mcp.prompt.McpPromptMessage;
import com.contextengine.mcp.prompt.McpPromptService;
import com.contextengine.mcp.stub.StubProjectRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpPromptsTest {

    private ProjectRepository projectRepository;
    private McpPromptService promptService;

    private ProjectId projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = ProjectId.generate();
        project = new Project(projectId, new Path("/repo"), "Prompt Project");
        project.activate();

        projectRepository = new StubProjectRepository() {
            @Override
            public Optional<Project> findById(ProjectId id) {
                if (projectId.equals(id)) {
                    return Optional.of(project);
                }
                return Optional.empty();
            }
        };
        promptService = new McpPromptService(projectRepository);
    }

    @Test
    void testListPrompts() {
        List<McpPrompt> prompts = promptService.listPrompts();
        Assertions.assertEquals(3, prompts.size());
        Assertions.assertTrue(prompts.stream().anyMatch(p -> p.name().equals("analyze-bug")));
    }

    @Test
    void testGetPromptAnalyzeBug() {
        BugId bugId = BugId.generate();
        Bug bug = new Bug(bugId, projectId, new Path("/repo/App.java"), 5, 10, "gitHash123");
        project.addBug(bug);

        List<McpPromptMessage> messages = promptService.getPrompt(
            "analyze-bug",
            Map.of(
                "projectId", projectId.value().toString(),
                "bugId", bugId.value().toString()
            )
        );

        Assertions.assertEquals(1, messages.size());
        Assertions.assertEquals("user", messages.get(0).role());
        Assertions.assertTrue(messages.get(0).content().text().contains("App.java"));
    }

    @Test
    void testGetPromptReviewArchitecture() {
        List<McpPromptMessage> messages = promptService.getPrompt(
            "review-architecture",
            Map.of("projectId", projectId.value().toString())
        );

        Assertions.assertEquals(1, messages.size());
        Assertions.assertTrue(messages.get(0).content().text().contains("Review the current architecture"));
    }

    @Test
    void testGetPromptMissingParameters() {
        Assertions.assertThrows(McpException.class, () -> 
            promptService.getPrompt("analyze-bug", Map.of("projectId", projectId.value().toString()))
        );
    }
}
