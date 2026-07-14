package com.contextengine.mcp.protocol;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.McpException;
import com.contextengine.mcp.prompt.McpPromptService;
import com.contextengine.mcp.resource.McpResourceContent;
import com.contextengine.mcp.resource.McpResourceService;
import com.contextengine.mcp.security.McpAuthenticationService;
import com.contextengine.mcp.security.McpAuthorizationService;
import com.contextengine.mcp.security.McpScopeValidator;
import com.contextengine.mcp.security.McpSecurityException;
import com.contextengine.mcp.session.McpSession;
import com.contextengine.mcp.session.McpSessionManager;
import com.contextengine.mcp.tool.McpTool;
import com.contextengine.infrastructure.metrics.TelemetryMetricsRegistry;
import com.contextengine.application.event.EventContext;
import org.slf4j.MDC;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Parses JSON-RPC request packets, routes execution to registered tools, resources, prompts, and sessions.
 * Integrates authentication, authorization, and scope validations on protocol entry.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / SRS-CE-FR-020
 * Responsibility: Parse inbound requests, route to the appropriate resource/prompt/session services or tool handlers,
 * enforce security parameters, and construct compliant JSON-RPC responses.
 * Dependencies: {@link McpRequest}, {@link McpResponse}, {@link McpErrorResponse}, {@link McpTool},
 * {@link McpResourceService}, {@link McpPromptService}, {@link McpSessionManager},
 * {@link McpAuthenticationService}, {@link McpAuthorizationService}, {@link McpScopeValidator}
 */
public class McpProtocolHandler {

    private final Map<String, McpTool> toolRegistry;
    private final McpResourceService resourceService;
    private final McpPromptService promptService;
    private final McpSessionManager sessionManager;
    private final McpAuthenticationService authenticationService;
    private final McpAuthorizationService authorizationService;
    private final McpScopeValidator scopeValidator;
    private final ProjectRepository projectRepository;
    private final com.contextengine.security.authorization.AuthorizationService coreAuthorizationService;
    private final TelemetryMetricsRegistry metricsRegistry;

    /**
     * Constructs the handler injecting tools and protocol support services.
     *
     * @param tools list of all supported MCP tool beans
     * @param resourceService service resolving resources
     * @param promptService service managing prompts
     * @param sessionManager service managing session lifecycle
     * @param authenticationService identity authentication service
     * @param authorizationService session permission authorization service
     * @param scopeValidator workspace boundary scope validator
     * @param projectRepository project repository interface
     */
    public McpProtocolHandler(
        List<McpTool> tools,
        McpResourceService resourceService,
        McpPromptService promptService,
        McpSessionManager sessionManager,
        McpAuthenticationService authenticationService,
        McpAuthorizationService authorizationService,
        McpScopeValidator scopeValidator,
        ProjectRepository projectRepository
    ) {
        this(
            tools,
            resourceService,
            promptService,
            sessionManager,
            authenticationService,
            authorizationService,
            scopeValidator,
            projectRepository,
            new com.contextengine.security.authorization.AuthorizationService(),
            null
        );
    }

    /**
     * Constructs the handler injecting tools and protocol support services.
     *
     * @param tools list of all supported MCP tool beans
     * @param resourceService service resolving resources
     * @param promptService service managing prompts
     * @param sessionManager service managing session lifecycle
     * @param authenticationService identity authentication service
     * @param authorizationService session permission authorization service
     * @param scopeValidator workspace boundary scope validator
     * @param projectRepository project repository interface
     * @param coreAuthorizationService core application scope authorization service
     */
    public McpProtocolHandler(
        List<McpTool> tools,
        McpResourceService resourceService,
        McpPromptService promptService,
        McpSessionManager sessionManager,
        McpAuthenticationService authenticationService,
        McpAuthorizationService authorizationService,
        McpScopeValidator scopeValidator,
        ProjectRepository projectRepository,
        com.contextengine.security.authorization.AuthorizationService coreAuthorizationService
    ) {
        this(
            tools,
            resourceService,
            promptService,
            sessionManager,
            authenticationService,
            authorizationService,
            scopeValidator,
            projectRepository,
            coreAuthorizationService,
            null
        );
    }

    /**
     * Constructs the handler injecting tools and protocol support services.
     *
     * @param tools list of all supported MCP tool beans
     * @param resourceService service resolving resources
     * @param promptService service managing prompts
     * @param sessionManager service managing session lifecycle
     * @param authenticationService identity authentication service
     * @param authorizationService session permission authorization service
     * @param scopeValidator workspace boundary scope validator
     * @param projectRepository project repository interface
     * @param coreAuthorizationService core application scope authorization service
     * @param metricsRegistry telemetry metrics registry component
     */
    public McpProtocolHandler(
        List<McpTool> tools,
        McpResourceService resourceService,
        McpPromptService promptService,
        McpSessionManager sessionManager,
        McpAuthenticationService authenticationService,
        McpAuthorizationService authorizationService,
        McpScopeValidator scopeValidator,
        ProjectRepository projectRepository,
        com.contextengine.security.authorization.AuthorizationService coreAuthorizationService,
        TelemetryMetricsRegistry metricsRegistry
    ) {
        this.toolRegistry = new HashMap<>();
        if (tools != null) {
            for (McpTool tool : tools) {
                this.toolRegistry.put(tool.getName(), tool);
            }
        }
        this.resourceService = Objects.requireNonNull(resourceService, "McpResourceService must not be null");
        this.promptService = Objects.requireNonNull(promptService, "McpPromptService must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "McpSessionManager must not be null");
        this.authenticationService = Objects.requireNonNull(authenticationService, "McpAuthenticationService must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "McpAuthorizationService must not be null");
        this.scopeValidator = Objects.requireNonNull(scopeValidator, "McpScopeValidator must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
        this.coreAuthorizationService = Objects.requireNonNull(coreAuthorizationService, "coreAuthorizationService must not be null");
        this.metricsRegistry = metricsRegistry;
    }

    /**
     * Handles an incoming JSON-RPC request, executes the matching tool/resource/prompt action, and returns the response.
     *
     * @param request the deserialized request envelope
     * @return the serialized JSON-RPC response (success or error)
     */
    @SuppressWarnings("unchecked")
    public Object handleRequest(McpRequest request) {
        if (request == null || !McpConstants.JSONRPC_VERSION.equals(request.getJsonrpc()) || request.getMethod() == null || request.getMethod().isEmpty()) {
            return new McpErrorResponse(
                McpConstants.JSONRPC_VERSION,
                new McpErrorResponse.McpError(McpConstants.ERROR_INVALID_REQUEST, "Invalid JSON-RPC request structure.", null),
                request != null ? request.getId() : null
            );
        }

        if (metricsRegistry != null) {
            metricsRegistry.incrementMcpRequest();
        }

        // Extract Correlation ID and Request ID
        String correlationId = null;
        String requestId = null;
        String traceId = null;
        String spanId = null;
        if (request.getParams() != null) {
            correlationId = (String) request.getParams().get("correlationId");
            if (correlationId == null) {
                correlationId = (String) request.getParams().get("X-Correlation-ID");
            }
            requestId = (String) request.getParams().get("clientRequestId");
            if (requestId == null) {
                requestId = (String) request.getParams().get("requestId");
            }
            traceId = (String) request.getParams().get("traceId");
            if (traceId == null) {
                traceId = (String) request.getParams().get("X-Trace-ID");
            }
            spanId = (String) request.getParams().get("spanId");
            if (spanId == null) {
                spanId = (String) request.getParams().get("X-Span-ID");
            }
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = correlationId;
        }
        if (spanId == null || spanId.trim().isEmpty()) {
            spanId = UUID.randomUUID().toString();
        }

        // Set to MDC and EventContext
        MDC.put("correlationId", correlationId);
        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        
        UUID correlationUuid;
        UUID traceUuid;
        UUID spanUuid;
        try {
            correlationUuid = UUID.fromString(correlationId);
        } catch (IllegalArgumentException e) {
            correlationUuid = UUID.randomUUID();
        }
        try {
            traceUuid = UUID.fromString(traceId);
        } catch (IllegalArgumentException e) {
            traceUuid = correlationUuid;
        }
        try {
            spanUuid = UUID.fromString(spanId);
        } catch (IllegalArgumentException e) {
            spanUuid = UUID.randomUUID();
        }
        
        EventContext.setCorrelationId(correlationUuid);
        EventContext.setTraceId(traceUuid);
        EventContext.setSpanId(spanUuid);

        try {
            Object result;
            String method = request.getMethod();

            // Resolve active session for non-initialize requests
            McpSession session = null;
            if (!"initialize".equals(method)) {
                session = resolveActiveSession(request);
                if (session == null && authenticationService.isAuthenticationRequired()) {
                    throw new McpSecurityException(4001, "Authentication failed: Missing or invalid session identifier");
                }
                if (session != null) {
                    com.contextengine.security.foundation.SecurityContext.setPrincipal(
                        new com.contextengine.security.foundation.SecurityContext.Principal(
                            session.getContext().getClientName(),
                            session.getContext().getPermissions()
                        )
                    );
                }
            }

            switch (method) {
                case "initialize":
                    result = handleInitialize(request);
                    break;

                case "notifications/initialized":
                    result = handleInitializedNotification(request);
                    break;

                case "resources/list":
                    if (session != null) {
                        authorizationService.authorize(session, "mcp:read");
                        coreAuthorizationService.checkPermission("mcp:read");
                    }
                    result = Map.of("resources", resourceService.listResources());
                    break;

                case "resources/read":
                    if (session != null) {
                        authorizationService.authorize(session, "mcp:read");
                        coreAuthorizationService.checkPermission("mcp:read");
                    }
                    result = handleResourcesRead(request, session);
                    break;

                case "prompts/list":
                    if (session != null) {
                        authorizationService.authorize(session, "mcp:read");
                        coreAuthorizationService.checkPermission("mcp:read");
                    }
                    result = Map.of("prompts", promptService.listPrompts());
                    break;

                case "prompts/get":
                    if (session != null) {
                        authorizationService.authorize(session, "mcp:read");
                        coreAuthorizationService.checkPermission("mcp:read");
                    }
                    result = handlePromptsGet(request, session);
                    break;

                case "tools/list":
                    List<Map<String, Object>> toolsList = toolRegistry.values().stream()
                        .map(tool -> Map.<String, Object>of(
                            "name", tool.getName(),
                            "description", "MCP Tool execution endpoint"
                        ))
                        .toList();
                    result = Map.of("tools", toolsList);
                    break;

                case "tools/call":
                    if (session != null) {
                        authorizationService.authorize(session, "mcp:tool_invoke");
                        coreAuthorizationService.checkPermission("mcp:tool_invoke");
                    }
                    result = handleToolsCall(request, session);
                    break;

                default:
                    // Fallback to direct method routing for backward compatibility
                    McpTool tool = toolRegistry.get(method);
                    if (tool != null) {
                        if (session != null) {
                            authorizationService.authorize(session, "mcp:tool_invoke");
                            coreAuthorizationService.checkPermission("mcp:tool_invoke");
                        }
                        result = tool.execute(request);
                    } else {
                        if (metricsRegistry != null) {
                            metricsRegistry.incrementMcpError();
                        }
                        return new McpErrorResponse(
                            McpConstants.JSONRPC_VERSION,
                            new McpErrorResponse.McpError(McpConstants.ERROR_METHOD_NOT_FOUND, "Method not found: " + method, null),
                            request.getId()
                        );
                    }
                    break;
            }

            return new McpResponse(McpConstants.JSONRPC_VERSION, result, request.getId());

        } catch (com.contextengine.security.foundation.SecurityException ex) {
            if (metricsRegistry != null) {
                metricsRegistry.incrementMcpError();
            }
            return new McpErrorResponse(
                McpConstants.JSONRPC_VERSION,
                new McpErrorResponse.McpError(ex.getErrorCode(), ex.getMessage(), ex.getDetails()),
                request.getId()
            );
        } catch (McpSecurityException ex) {
            if (metricsRegistry != null) {
                metricsRegistry.incrementMcpError();
            }
            return new McpErrorResponse(
                McpConstants.JSONRPC_VERSION,
                new McpErrorResponse.McpError(ex.getErrorCode(), ex.getMessage(), ex.getDetails()),
                request.getId()
            );
        } catch (McpException ex) {
            if (metricsRegistry != null) {
                metricsRegistry.incrementMcpError();
            }
            return new McpErrorResponse(
                McpConstants.JSONRPC_VERSION,
                new McpErrorResponse.McpError(ex.getErrorCode(), ex.getMessage(), ex.getDetails()),
                request.getId()
            );
        } catch (Exception ex) {
            if (metricsRegistry != null) {
                metricsRegistry.incrementMcpError();
            }
            return new McpErrorResponse(
                McpConstants.JSONRPC_VERSION,
                new McpErrorResponse.McpError(McpConstants.ERROR_INTERNAL_ERROR, ex.getMessage() != null ? ex.getMessage() : "An unexpected execution error occurred.", null),
                request.getId()
            );
        } finally {
            com.contextengine.security.foundation.SecurityContext.clear();
            MDC.remove("correlationId");
            MDC.remove("requestId");
            MDC.remove("traceId");
            MDC.remove("spanId");
            EventContext.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private Object handleInitialize(McpRequest request) {
        Map<String, Object> params = request.getParams();
        String clientName = "unknown";
        String clientVersion = "1.0.0";
        String token = null;

        if (params != null) {
            if (params.containsKey("clientInfo")) {
                Map<String, Object> clientInfo = (Map<String, Object>) params.get("clientInfo");
                if (clientInfo != null) {
                    clientName = (String) clientInfo.getOrDefault("name", "unknown");
                    clientVersion = (String) clientInfo.getOrDefault("version", "1.0.0");
                }
            }
            token = (String) params.get("token");
            if (token == null) {
                token = (String) params.get("securityToken");
            }
        }

        McpSession session = authenticationService.authenticate(token, clientName, clientVersion);

        return Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "resources", Map.of(),
                "prompts", Map.of(),
                "tools", Map.of()
            ),
            "serverInfo", Map.of(
                "name", "context-engine-server",
                "version", "0.0.1"
            ),
            "sessionId", session.getSessionId()
        );
    }

    private Object handleInitializedNotification(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params != null && params.containsKey("sessionId")) {
            String sessionId = (String) params.get("sessionId");
            sessionManager.establishSession(sessionId);
        }
        return Map.of("status", "success");
    }

    private Object handleResourcesRead(McpRequest request, McpSession session) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("uri")) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameter: uri");
        }
        String uri = (String) params.get("uri");
        McpResourceContent content = resourceService.readResource(uri);
        return Map.of("contents", List.of(content));
    }

    @SuppressWarnings("unchecked")
    private Object handlePromptsGet(McpRequest request, McpSession session) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("name")) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameter: name");
        }
        String promptName = (String) params.get("name");
        Map<String, String> args = (Map<String, String>) params.get("arguments");

        if (args != null && args.containsKey("projectId") && session != null) {
            ProjectId projId = ProjectId.fromString(args.get("projectId"));
            Optional<Project> projectOpt = projectRepository.findById(projId);
            projectOpt.ifPresent(p -> scopeValidator.validateWorkspaceAccess(session, p));
        }

        return Map.of(
            "description", "Generated prompt template for: " + promptName,
            "messages", promptService.getPrompt(promptName, args)
        );
    }

    @SuppressWarnings("unchecked")
    private Object handleToolsCall(McpRequest request, McpSession session) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("name")) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Missing required parameter: name");
        }
        String toolName = (String) params.get("name");
        Map<String, Object> toolArgs = (Map<String, Object>) params.get("arguments");

        McpTool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new McpException(McpConstants.ERROR_METHOD_NOT_FOUND, "Tool not found: " + toolName);
        }

        // Verify write privileges on modifying actions
        if (toolName.contains("register") || toolName.contains("submit") || toolName.contains("propose") || toolName.contains("create")) {
            if (session != null) {
                authorizationService.authorize(session, "mcp:write_graph");
                coreAuthorizationService.checkPermission("mcp:write_graph");
            }
        }

        // Verify spatial boundaries if projectId is passed
        if (toolArgs != null && toolArgs.containsKey("projectId") && session != null) {
            String projIdStr = (String) toolArgs.get("projectId");
            ProjectId projId = ProjectId.fromString(projIdStr);
            Optional<Project> projectOpt = projectRepository.findById(projId);
            projectOpt.ifPresent(p -> scopeValidator.validateWorkspaceAccess(session, p));
        }

        McpRequest toolRequest = new McpRequest(
            request.getJsonrpc(),
            toolName,
            toolArgs,
            request.getId()
        );

        return tool.execute(toolRequest);
    }

    private McpSession resolveActiveSession(McpRequest request) {
        Map<String, Object> params = request.getParams();
        String sessionId = null;
        if (params != null) {
            sessionId = (String) params.get("sessionId");
        }
        if (sessionId != null) {
            return sessionManager.getSession(sessionId).orElse(null);
        }
        // Fallback to the single active session if exactly one exists (implicit context)
        if (sessionManager.getActiveSessions().size() == 1) {
            return sessionManager.getActiveSessions().iterator().next();
        }
        return null;
    }
}
