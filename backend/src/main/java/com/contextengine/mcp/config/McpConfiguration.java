package com.contextengine.mcp.config;

import com.contextengine.configuration.properties.McpProperties;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.mcp.protocol.McpProtocolHandler;
import com.contextengine.mcp.security.McpAuthenticationService;
import com.contextengine.mcp.security.McpAuthorizationService;
import com.contextengine.mcp.security.McpScopeValidator;
import com.contextengine.mcp.session.McpSessionManager;
import com.contextengine.mcp.transport.HttpTransport;
import com.contextengine.mcp.transport.StdioTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Spring configuration class that registers and wires the MCP security and transport subsystem components.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Phase 8.12.9
 * Responsibility: Setup security validators, initialize HTTP and Stdio transports, and control start/stop lifecycles.
 * Dependencies: {@link McpProperties}, {@link McpProtocolHandler}, {@link McpSessionManager}, {@link ProjectRepository}
 * Future Usage: Bootstrapped automatically by Spring Boot application context startup.
 */
@Configuration
public class McpConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    private final McpProperties mcpProperties;
    private final McpProtocolHandler protocolHandler;
    private final McpSessionManager sessionManager;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;
    private StdioTransport stdioTransport;

    /**
     * Constructs a McpConfiguration.
     *
     * @param mcpProperties MCP property holder
     * @param protocolHandler protocol execution handler
     * @param sessionManager session tracking manager
     * @param projectRepository project repository
     * @param objectMapper JSON utility mapper
     */
    public McpConfiguration(
        McpProperties mcpProperties,
        @org.springframework.context.annotation.Lazy McpProtocolHandler protocolHandler,
        McpSessionManager sessionManager,
        ProjectRepository projectRepository,
        ObjectMapper objectMapper
    ) {
        this.mcpProperties = Objects.requireNonNull(mcpProperties, "McpProperties must not be null");
        this.protocolHandler = Objects.requireNonNull(protocolHandler, "McpProtocolHandler must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "McpSessionManager must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");
    }

    @Bean
    public McpAuthenticationService mcpAuthenticationService() {
        return new McpAuthenticationService(mcpProperties.securityTokens(), sessionManager);
    }

    @Bean
    public McpAuthorizationService mcpAuthorizationService() {
        return new McpAuthorizationService();
    }

    @Bean
    public McpScopeValidator mcpScopeValidator() {
        return new McpScopeValidator();
    }

    @Bean
    public HttpTransport httpTransport() {
        return new HttpTransport(protocolHandler);
    }

    @Bean(destroyMethod = "stop")
    public StdioTransport stdioTransport() {
        this.stdioTransport = new StdioTransport(protocolHandler, objectMapper);
        return this.stdioTransport;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Automatically start the standard I/O listener thread if enabled in properties
        if (Boolean.TRUE.equals(mcpProperties.stdioEnabled()) && stdioTransport != null) {
            stdioTransport.start();
        }
    }
}
