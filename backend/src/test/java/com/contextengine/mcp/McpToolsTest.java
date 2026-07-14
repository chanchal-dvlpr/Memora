package com.contextengine.mcp;

import com.contextengine.application.command.GenerateContextCommand;
import com.contextengine.application.command.RegisterProjectCommand;
import com.contextengine.application.command.ScanProjectCommand;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.dto.ProjectDto;
import com.contextengine.application.dto.KnowledgeNodeDto;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.application.usecase.SearchUseCase;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.mcp.protocol.McpRequest;
import com.contextengine.mcp.tool.CoreSearchTool;
import com.contextengine.mcp.tool.GenerateContextSnapshotTool;
import com.contextengine.mcp.tool.GenerateContextTool;
import com.contextengine.mcp.tool.RegisterProjectTool;
import com.contextengine.mcp.tool.ScanProjectTool;
import com.contextengine.mcp.tool.SearchTool;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class McpToolsTest {

    @Test
    @SuppressWarnings("unchecked")
    void testRegisterProjectToolSuccess() throws Exception {
        ProjectApplicationService service = Mockito.mock(ProjectApplicationService.class);
        ProjectDto mockDto = new ProjectDto("projectId", new File(".").getCanonicalPath(), "Title", Collections.emptyList());
        Mockito.when(service.registerProject(Mockito.any(RegisterProjectCommand.class)))
                .thenReturn(ApplicationResult.success(mockDto));

        RegisterProjectTool tool = new RegisterProjectTool(service);
        Assertions.assertEquals("context_engine_register_project", tool.getName());

        McpRequest request = new McpRequest("2.0", "context_engine_register_project", Map.of("targetPath", "."), 1);
        Object response = tool.execute(request);

        Assertions.assertTrue(response instanceof Map);
        Map<String, Object> map = (Map<String, Object>) response;
        Assertions.assertEquals(false, map.get("isError"));
        Assertions.assertEquals("projectId", map.get("projectId"));
        Assertions.assertEquals("Title", map.get("projectName"));
    }

    @Test
    void testRegisterProjectToolMissingParams() {
        ProjectApplicationService service = Mockito.mock(ProjectApplicationService.class);
        RegisterProjectTool tool = new RegisterProjectTool(service);

        McpRequest request = new McpRequest("2.0", "context_engine_register_project", Map.of(), 1);
        Assertions.assertThrows(McpException.class, () -> tool.execute(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testScanProjectToolSuccess() {
        ProjectApplicationService service = Mockito.mock(ProjectApplicationService.class);
        Mockito.when(service.scanProject(Mockito.any(ScanProjectCommand.class)))
                .thenReturn(ApplicationResult.success(true));

        ScanProjectTool tool = new ScanProjectTool(service);
        Assertions.assertEquals("context_engine_scan_project", tool.getName());

        UUID uuid = UUID.randomUUID();
        McpRequest request = new McpRequest("2.0", "context_engine_scan_project", Map.of("projectId", uuid.toString()), 1);
        Object response = tool.execute(request);

        Assertions.assertTrue(response instanceof Map);
        Map<String, Object> map = (Map<String, Object>) response;
        Assertions.assertEquals(false, map.get("isError"));
        Assertions.assertEquals(true, map.get("success"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGenerateContextToolSuccess() {
        ContextApplicationService service = Mockito.mock(ContextApplicationService.class);
        ContextSnapshotDto mockDto = new ContextSnapshotDto("snapshotId", "projectId", "payload bytes", 120, "2026-07-12T12:00:00Z");
        Mockito.when(service.generateContext(Mockito.any(GenerateContextCommand.class)))
                .thenReturn(ApplicationResult.success(mockDto));

        GenerateContextTool tool = new GenerateContextTool(service);
        Assertions.assertEquals("generate_context", tool.getName());

        UUID projectUuid = UUID.randomUUID();
        UUID nodeUuid = UUID.randomUUID();
        McpRequest request = new McpRequest("2.0", "generate_context", Map.of(
                "projectId", projectUuid.toString(),
                "contextQuery", "test focus",
                "tokenBudget", 5000,
                "focusNode", nodeUuid.toString(),
                "format", "markdown"
        ), 1);

        Object response = tool.execute(request);
        Assertions.assertTrue(response instanceof Map);
        Map<String, Object> map = (Map<String, Object>) response;
        Assertions.assertEquals(false, map.get("isError"));
        Assertions.assertEquals("snapshotId", map.get("contextSessionId"));
        Assertions.assertEquals("MARKDOWN", map.get("targetViewProfile"));
        Assertions.assertEquals(120, map.get("tokensConsumed"));
        Assertions.assertEquals("payload bytes", map.get("contextPayload"));

        GenerateContextSnapshotTool snapshotTool = new GenerateContextSnapshotTool(service);
        Assertions.assertEquals("generate_context_snapshot", snapshotTool.getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchToolSuccess() {
        SearchUseCase searchUseCase = Mockito.mock(SearchUseCase.class);
        ProjectId projectId = ProjectId.generate();
        KnowledgeNodeDto mockDto = new KnowledgeNodeDto("nodeId", "FILE", Map.of("name", "TestClass.java"));

        Mockito.when(searchUseCase.execute(Mockito.any()))
                .thenReturn(ApplicationResult.success(List.of(mockDto)));

        SearchTool tool = new SearchTool(searchUseCase);
        Assertions.assertEquals("search_engineering_memory", tool.getName());

        McpRequest request = new McpRequest("2.0", "search_engineering_memory", Map.of(
                "projectId", projectId.value().toString(),
                "query_string", "TestClass"
        ), 1);

        Object response = tool.execute(request);
        Assertions.assertTrue(response instanceof Map);
        Map<String, Object> map = (Map<String, Object>) response;
        Assertions.assertEquals(false, map.get("isError"));
        List<Map<String, Object>> results = (List<Map<String, Object>>) map.get("results");
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("nodeId", results.get(0).get("id"));

        CoreSearchTool coreSearchTool = new CoreSearchTool(searchUseCase);
        Assertions.assertEquals("core_search", coreSearchTool.getName());
    }
}
