package com.contextengine.security;

import com.contextengine.security.authentication.AuthenticationService;
import com.contextengine.security.authorization.AuthorizationService;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.rest.SecurityInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SecurityInterceptor}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (REST Security Interceptor Tests)
 * Purpose: Validate token extraction from request headers, method classification, and scope checks.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SecurityInterceptorTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private SecurityInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SecurityInterceptor(authenticationService, authorizationService);
    }

    @Test
    void testPreHandleGetMethodSuccess() throws Exception {
        Mockito.when(request.getHeader("X-Session-Token")).thenReturn("token-123");
        Mockito.when(request.getMethod()).thenReturn("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        Assertions.assertTrue(result);
        Mockito.verify(authenticationService).authenticate("token-123", "rest-client");
        Mockito.verify(authorizationService).checkPermission(SecurityConstants.SCOPE_READ);
    }

    @Test
    void testPreHandlePostMethodSuccess() throws Exception {
        Mockito.when(request.getHeader("X-Session-Token")).thenReturn(null);
        Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer bearer-token-xyz");
        Mockito.when(request.getMethod()).thenReturn("POST");

        boolean result = interceptor.preHandle(request, response, new Object());

        Assertions.assertTrue(result);
        Mockito.verify(authenticationService).authenticate("bearer-token-xyz", "rest-client");
        Mockito.verify(authorizationService).checkPermission(SecurityConstants.SCOPE_TOOL_INVOKE);
    }

    @Test
    void testAfterCompletionClearsContext() throws Exception {
        interceptor.afterCompletion(request, response, new Object(), null);
        Mockito.verify(authenticationService).clear();
    }
}
