package com.contextengine.mcp;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.mcp.protocol.McpProtocolHandler;
import com.contextengine.mcp.protocol.McpRequest;
import com.contextengine.mcp.security.McpAuthenticationService;
import com.contextengine.mcp.security.McpAuthorizationService;
import com.contextengine.mcp.security.McpScopeValidator;
import com.contextengine.mcp.security.McpSecurityException;
import com.contextengine.mcp.session.McpSession;
import com.contextengine.mcp.session.McpSessionManager;
import com.contextengine.mcp.transport.HttpTransport;
import com.contextengine.mcp.transport.StdioTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class McpSecurityAndTransportTest {

    private McpSessionManager sessionManager;
    private McpAuthenticationService authenticationService;
    private McpAuthorizationService authorizationService;
    private McpScopeValidator scopeValidator;

    @BeforeEach
    void setUp() {
        sessionManager = new McpSessionManager();
        authenticationService = new McpAuthenticationService(List.of("valid-key-123"), sessionManager);
        authorizationService = new McpAuthorizationService();
        scopeValidator = new McpScopeValidator();
    }

    private McpProtocolHandler createRealHandler() {
        ProjectRepository projectRepository = new com.contextengine.mcp.stub.StubProjectRepository();
        com.contextengine.domain.repository.ContextRepository contextRepository = new com.contextengine.mcp.stub.StubContextRepository();
        com.contextengine.domain.repository.KnowledgeGraphRepository graphRepository = new com.contextengine.mcp.stub.StubKnowledgeGraphRepository();

        com.contextengine.mcp.resource.McpResourceService resourceService = new com.contextengine.mcp.resource.McpResourceService(projectRepository, contextRepository, graphRepository) {
            @Override
            public List<com.contextengine.mcp.resource.McpResource> listResources() {
                return Collections.emptyList();
            }
        };

        com.contextengine.mcp.prompt.McpPromptService promptService = new com.contextengine.mcp.prompt.McpPromptService(projectRepository) {
            @Override
            public List<com.contextengine.mcp.prompt.McpPrompt> listPrompts() {
                return Collections.emptyList();
            }
        };

        return new McpProtocolHandler(
            Collections.emptyList(), resourceService, promptService, sessionManager,
            authenticationService, authorizationService, scopeValidator, projectRepository
        );
    }

    @Test
    void testAuthenticationSuccess() {
        McpSession session = authenticationService.authenticate("valid-key-123", "IDE", "1.0");
        Assertions.assertNotNull(session);
        Assertions.assertEquals(McpSessionState.AUTHENTICATED, session.getState());
        Assertions.assertTrue(session.getContext().hasPermission("mcp:read"));
    }

    @Test
    void testAuthenticationFailure() {
        Assertions.assertThrows(McpSecurityException.class, () ->
            authenticationService.authenticate("invalid-key", "IDE", "1.0")
        );
        Assertions.assertThrows(McpSecurityException.class, () ->
            authenticationService.authenticate(null, "IDE", "1.0")
        );
    }

    @Test
    void testAuthorization() {
        McpSession session = authenticationService.authenticate("valid-key-123", "IDE", "1.0");

        // Should pass
        authorizationService.authorize(session, "mcp:read");

        // Should fail
        Assertions.assertThrows(McpSecurityException.class, () ->
            authorizationService.authorize(session, "mcp:unauthorized_scope")
        );
    }

    @Test
    void testScopeValidatorArchived() {
        McpSession session = authenticationService.authenticate("valid-key-123", "IDE", "1.0");
        Project project = new Project(ProjectId.generate(), new Path("/repo"), "Archived Project");
        project.activate();
        project.archive(); // State -> ARCHIVED

        Assertions.assertThrows(McpSecurityException.class, () ->
            scopeValidator.validateWorkspaceAccess(session, project)
        );
    }

    @Test
    void testHttpTransportLoopbackCheck() {
        McpProtocolHandler realHandler = createRealHandler();
        HttpTransport httpTransport = new HttpTransport(realHandler);

        McpRequest req = new McpRequest("2.0", "initialize", Map.of(
            "clientInfo", Map.of("name", "test", "version", "1.0.0")
        ), 1);

        // Use a mutable array to control the remote address returned by the JDK dynamic proxy
        final String[] remoteAddrHolder = new String[1];
        HttpServletRequest mockServletRequest = (HttpServletRequest) Proxy.newProxyInstance(
            HttpServletRequest.class.getClassLoader(),
            new Class<?>[]{HttpServletRequest.class},
            (proxy, method, args) -> {
                if ("getRemoteAddr".equals(method.getName())) {
                    return remoteAddrHolder[0];
                }
                return null;
            }
        );

        // Case 1: External IP (Forbidden)
        remoteAddrHolder[0] = "192.168.1.5";
        ResponseEntity<Object> resp = httpTransport.handleMcpRequest(req, mockServletRequest);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());

        // Case 2: Loopback IP (Success)
        remoteAddrHolder[0] = "127.0.0.1";
        resp = httpTransport.handleMcpRequest(req, mockServletRequest);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testStdioTransportStartStop() {
        McpProtocolHandler realHandler = createRealHandler();
        ObjectMapper mapper = new ObjectMapper();
        StdioTransport stdioTransport = new StdioTransport(realHandler, mapper);

        stdioTransport.start();
        Assertions.assertTrue(stdioTransport.isRunning());
        stdioTransport.stop();
        Assertions.assertFalse(stdioTransport.isRunning());
    }
}
