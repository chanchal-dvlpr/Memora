package com.contextengine.security;

import com.contextengine.security.authorization.AuthorizationService;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityContext;
import com.contextengine.security.foundation.SecurityException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuthorizationService}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Authorization Tests)
 * Purpose: Validate scope permission checks and unauthorized access rejections.
 * </p>
 */
class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void testCheckPermissionSuccess() {
        SecurityContext.Principal principal = new SecurityContext.Principal("test-agent", Set.of(SecurityConstants.SCOPE_READ));
        SecurityContext.setPrincipal(principal);

        Assertions.assertDoesNotThrow(() -> {
            authorizationService.checkPermission(SecurityConstants.SCOPE_READ);
        });
    }

    @Test
    void testCheckPermissionLackingScopeThrows() {
        SecurityContext.Principal principal = new SecurityContext.Principal("test-agent", Set.of(SecurityConstants.SCOPE_READ));
        SecurityContext.setPrincipal(principal);

        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            authorizationService.checkPermission(SecurityConstants.SCOPE_TOOL_INVOKE);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_SCOPE_INSUFFICIENT, exception.getErrorCode());
    }

    @Test
    void testCheckPermissionUnauthenticatedThrows() {
        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            authorizationService.checkPermission(SecurityConstants.SCOPE_READ);
        });

        Assertions.assertEquals(SecurityConstants.ERROR_AUTHENTICATION_FAILED, exception.getErrorCode());
    }
}
