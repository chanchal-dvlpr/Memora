package com.contextengine.mcp;

import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.mcp.prompt.McpPromptService;
import com.contextengine.mcp.protocol.McpErrorResponse;
import com.contextengine.mcp.protocol.McpProtocolHandler;
import com.contextengine.mcp.protocol.McpRequest;
import com.contextengine.mcp.protocol.McpResponse;
import com.contextengine.mcp.resource.McpResourceService;
import com.contextengine.mcp.security.McpAuthenticationService;
import com.contextengine.mcp.security.McpAuthorizationService;
import com.contextengine.mcp.security.McpScopeValidator;
import com.contextengine.mcp.session.McpSessionManager;
import com.contextengine.mcp.tool.McpTool;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class McpProtocolTest {

    private McpResourceService resourceService;
    private McpPromptService promptService;
    private McpSessionManager sessionManager;
    private McpAuthenticationService authenticationService;
    private McpAuthorizationService authorizationService;
    private McpScopeValidator scopeValidator;
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        projectRepository = new com.contextengine.mcp.stub.StubProjectRepository();
        com.contextengine.domain.repository.ContextRepository contextRepository = new com.contextengine.mcp.stub.StubContextRepository();
        com.contextengine.domain.repository.KnowledgeGraphRepository graphRepository = new com.contextengine.mcp.stub.StubKnowledgeGraphRepository();

        resourceService = new McpResourceService(projectRepository, contextRepository, graphRepository) {
            @Override
            public List<com.contextengine.mcp.resource.McpResource> listResources() {
                return Collections.emptyList();
            }
            @Override
            public com.contextengine.mcp.resource.McpResourceContent readResource(String uri) {
                return new com.contextengine.mcp.resource.McpResourceContent(uri, "text/plain", "dummy resource content");
            }
        };

        promptService = new McpPromptService(projectRepository) {
            @Override
            public List<com.contextengine.mcp.prompt.McpPrompt> listPrompts() {
                return Collections.emptyList();
            }
            @Override
            public List<com.contextengine.mcp.prompt.McpPromptMessage> getPrompt(String name, Map<String, String> arguments) {
                return Collections.emptyList();
            }
        };

        sessionManager = new McpSessionManager();
        authenticationService = new McpAuthenticationService(Collections.emptyList(), sessionManager);
        authorizationService = new McpAuthorizationService();
        scopeValidator = new McpScopeValidator();
    }

    @Test
    void testMcpConstantsAndLifecycleState() {
        Assertions.assertEquals("2.0", McpConstants.JSONRPC_VERSION);
        Assertions.assertEquals("context_engine_register_project", McpConstants.TOOL_REGISTER_PROJECT);

        McpContext context = new McpContext("TestClient", "1.0.0");
        Assertions.assertEquals("TestClient", context.getClientName());
        Assertions.assertEquals("1.0.0", context.getClientVersion());
        Assertions.assertEquals(McpSessionState.DISCOVERED, context.getState());

        context.transitionTo(McpSessionState.CONNECTED);
        Assertions.assertEquals(McpSessionState.CONNECTED, context.getState());

        context.grantPermission("mcp:read");
        Assertions.assertTrue(context.hasPermission("mcp:read"));
        Assertions.assertFalse(context.hasPermission("mcp:write"));
    }

    @Test
    void testExceptionDetails() {
        Map<String, Object> details = Map.of("field", "targetPath", "error", "must be absolute");
        McpException ex = new McpException(McpConstants.ERROR_INVALID_PARAMS, "Invalid path", details);
        Assertions.assertEquals(McpConstants.ERROR_INVALID_PARAMS, ex.getErrorCode());
        Assertions.assertEquals("Invalid path", ex.getMessage());
        Assertions.assertEquals("targetPath", ex.getDetails().get("field"));
    }

    @Test
    void testProtocolHandlerInvalidRequest() {
        McpProtocolHandler handler = new McpProtocolHandler(
            Collections.emptyList(), resourceService, promptService, sessionManager,
            authenticationService, authorizationService, scopeValidator, projectRepository
        );

        // Null request
        Object response = handler.handleRequest(null);
        Assertions.assertTrue(response instanceof McpErrorResponse);
        McpErrorResponse errorResp = (McpErrorResponse) response;
        Assertions.assertEquals(McpConstants.ERROR_INVALID_REQUEST, errorResp.getError().code());

        // Invalid jsonrpc version
        McpRequest invalidVersion = new McpRequest("1.0", "some_method", Map.of(), 1);
        response = handler.handleRequest(invalidVersion);
        Assertions.assertTrue(response instanceof McpErrorResponse);
        errorResp = (McpErrorResponse) response;
        Assertions.assertEquals(McpConstants.ERROR_INVALID_REQUEST, errorResp.getError().code());

        // Empty method
        McpRequest emptyMethod = new McpRequest("2.0", "", Map.of(), 1);
        response = handler.handleRequest(emptyMethod);
        Assertions.assertTrue(response instanceof McpErrorResponse);
        errorResp = (McpErrorResponse) response;
        Assertions.assertEquals(McpConstants.ERROR_INVALID_REQUEST, errorResp.getError().code());
    }

    @Test
    void testProtocolHandlerMethodNotFound() {
        McpProtocolHandler handler = new McpProtocolHandler(
            Collections.emptyList(), resourceService, promptService, sessionManager,
            authenticationService, authorizationService, scopeValidator, projectRepository
        );
        McpRequest request = new McpRequest("2.0", "unknown_method", Map.of(), 1);
        Object response = handler.handleRequest(request);
        Assertions.assertTrue(response instanceof McpErrorResponse);
        McpErrorResponse errorResp = (McpErrorResponse) response;
        Assertions.assertEquals(McpConstants.ERROR_METHOD_NOT_FOUND, errorResp.getError().code());
    }

    @Test
    void testProtocolHandlerSuccessAndExceptionDispatch() {
        class MutableTool implements McpTool {
            boolean throwException = false;
            boolean throwGeneral = false;

            @Override
            public String getName() {
                return "mock_tool";
            }

            @Override
            public Object execute(McpRequest request) {
                if (throwException) {
                    throw new McpException(McpConstants.ERROR_CONSTRAINT_BREACH, "Constraint breached");
                }
                if (throwGeneral) {
                    throw new RuntimeException("Database error");
                }
                return Map.of("status", "success");
            }
        }

        MutableTool mockTool = new MutableTool();

        McpProtocolHandler handler = new McpProtocolHandler(
            List.of(mockTool), resourceService, promptService, sessionManager,
            authenticationService, authorizationService, scopeValidator, projectRepository
        );

        // Success execution
        McpRequest request = new McpRequest("2.0", "mock_tool", Map.of(), 42);
        Object response = handler.handleRequest(request);
        Assertions.assertTrue(response instanceof McpResponse);
        McpResponse mcpResp = (McpResponse) response;
        Assertions.assertEquals(42, mcpResp.getId());
        Map<String, Object> result = (Map<String, Object>) mcpResp.getResult();
        Assertions.assertEquals("success", result.get("status"));

        // McpException execution failure
        mockTool.throwException = true;
        response = handler.handleRequest(request);
        Assertions.assertTrue(response instanceof McpErrorResponse);
        McpErrorResponse errorResp = (McpErrorResponse) response;
        Assertions.assertEquals(McpConstants.ERROR_CONSTRAINT_BREACH, errorResp.getError().code());
        Assertions.assertEquals("Constraint breached", errorResp.getError().message());

        // General Exception execution failure
        mockTool.throwException = false;
        mockTool.throwGeneral = true;
        response = handler.handleRequest(request);
        Assertions.assertTrue(response instanceof McpErrorResponse);
        errorResp = (McpErrorResponse) response;
        Assertions.assertEquals(McpConstants.ERROR_INTERNAL_ERROR, errorResp.getError().code());
        Assertions.assertEquals("Database error", errorResp.getError().message());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProtocolHandlerStandardMCPMethods() {
        McpSessionManager realSessionManager = new McpSessionManager();
        McpAuthenticationService realAuthService = new McpAuthenticationService(Collections.emptyList(), realSessionManager);
        McpProtocolHandler handler = new McpProtocolHandler(
            Collections.emptyList(), resourceService, promptService, realSessionManager,
            realAuthService, authorizationService, scopeValidator, projectRepository
        );

        // test initialize
        McpRequest initRequest = new McpRequest("2.0", "initialize", Map.of(
            "clientInfo", Map.of("name", "test", "version", "1.0.0")
        ), 1);
        Object response = handler.handleRequest(initRequest);
        Assertions.assertTrue(response instanceof McpResponse);
        McpResponse mcpResponse = (McpResponse) response;
        Map<String, Object> result = (Map<String, Object>) mcpResponse.getResult();
        Assertions.assertEquals("2024-11-05", result.get("protocolVersion"));
        String sessionId = (String) result.get("sessionId");
        Assertions.assertNotNull(sessionId);

        // test notifications/initialized
        McpRequest initializedRequest = new McpRequest("2.0", "notifications/initialized", Map.of(
            "sessionId", sessionId
        ), 2);
        handler.handleRequest(initializedRequest);

        Assertions.assertEquals(1, realSessionManager.getActiveSessions().size());
        Assertions.assertEquals(McpSessionState.ACTIVE, realSessionManager.getSession(sessionId).orElseThrow().getState());

        // test resources/list
        McpRequest listResRequest = new McpRequest("2.0", "resources/list", Map.of(), 3);
        Object listResResponse = handler.handleRequest(listResRequest);
        Assertions.assertTrue(listResResponse instanceof McpResponse);

        // test prompts/list
        McpRequest listPromptsRequest = new McpRequest("2.0", "prompts/list", Map.of(), 4);
        Object listPromptsResponse = handler.handleRequest(listPromptsRequest);
        Assertions.assertTrue(listPromptsResponse instanceof McpResponse);
    }
}
