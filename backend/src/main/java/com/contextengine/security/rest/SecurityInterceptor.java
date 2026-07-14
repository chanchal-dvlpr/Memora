package com.contextengine.security.rest;

import com.contextengine.security.authentication.AuthenticationService;
import com.contextengine.security.authorization.AuthorizationService;
import com.contextengine.security.foundation.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC HandlerInterceptor executing token authentication and scope-based authorization checks on HTTP requests.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (REST Security)
 * Responsibility: Extract security credentials, authenticate clients, enforce operation-level access policies, and clean up thread-locals.
 * Dependencies: {@link AuthenticationService}, {@link AuthorizationService}, {@link SecurityConstants}
 * Future Usage: Integrated in WebMvcConfigurer registry to intercept REST presentation layer routes.
 * </p>
 */
public class SecurityInterceptor implements HandlerInterceptor {

    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    /**
     * Constructs a SecurityInterceptor.
     *
     * @param authenticationService service managing authentication and context setup
     * @param authorizationService service validating active context permissions
     */
    public SecurityInterceptor(
        AuthenticationService authenticationService,
        AuthorizationService authorizationService
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "AuthenticationService must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // Resolve security token from headers
            String token = extractToken(request);

            // Perform authentication (binds Principal to ThreadLocal SecurityContext)
            authenticationService.authenticate(token, "rest-client");

            // Enforce authorization checks based on the HTTP method type
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                authorizationService.checkPermission(SecurityConstants.SCOPE_READ);
            } else {
                authorizationService.checkPermission(SecurityConstants.SCOPE_TOOL_INVOKE);
            }

            return true;
        } catch (Throwable t) {
            authenticationService.clear();
            throw t;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Prevent ThreadLocal memory leaks in application server threads
        authenticationService.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("X-Session-Token");
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.trim().toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }
}
