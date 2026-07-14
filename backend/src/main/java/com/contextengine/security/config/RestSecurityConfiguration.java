package com.contextengine.security.config;

import com.contextengine.security.authentication.AuthenticationService;
import com.contextengine.security.authorization.AuthorizationService;
import com.contextengine.security.rest.SecurityInterceptor;
import java.util.Objects;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Configuration class implementing WebMvcConfigurer to register the Rest Presentation Security Interceptor.
 * Excludes Swagger, OpenAPI documentation, Actuator endpoints, and the sovereign MCP loopback endpoint.
 *
 * <p>Architecture Reference: Spring Boot Backend Design / Section ID 4.4 (REST Security Config)
 * Responsibility: Secure REST presentation mapping paths while bypassing documentation and protocol endpoints.
 * Dependencies: {@link AuthenticationService}, {@link AuthorizationService}, {@link SecurityInterceptor}
 * Future Usage: Bootstrapped by the Spring context to guard all incoming MVC servlet endpoints.
 * </p>
 */
@Configuration
public class RestSecurityConfiguration implements WebMvcConfigurer {

    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    /**
     * Constructs the RestSecurityConfiguration.
     *
     * @param authenticationService service managing authentication sessions
     * @param authorizationService service enforcing scope permissions
     */
    public RestSecurityConfiguration(
        AuthenticationService authenticationService,
        AuthorizationService authorizationService
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "AuthenticationService must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityInterceptor(authenticationService, authorizationService))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/v1/mcp", // MCP protocol has its own built-in credentials validation
                    "/api/v1/metrics", // Telemetry metrics are public/low-overhead
                    "/api/v1/dashboard", // Quality metric dashboard is public/low-overhead
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/h2-console/**",
                    "/actuator/**"
                );
    }
}
