package com.contextengine.security;

import com.contextengine.security.authentication.AuthenticationService;
import com.contextengine.security.authentication.CredentialValidator;
import com.contextengine.security.foundation.SecurityConstants;
import com.contextengine.security.foundation.SecurityContext;
import com.contextengine.security.foundation.SecurityException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuthenticationService} and {@link CredentialValidator}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Authentication Tests)
 * Purpose: Validate token authentication, failure rejections, and context binding.
 * </p>
 */
class AuthenticationServiceTest {

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        CredentialValidator validator = new CredentialValidator(List.of("valid-token-123"));
        authenticationService = new AuthenticationService(validator);
    }

    @AfterEach
    void tearDown() {
        authenticationService.clear();
    }

    @Test
    void testAuthenticateSuccess() {
        authenticationService.authenticate("valid-token-123", "test-agent");

        Optional<SecurityContext.Principal> principalOpt = SecurityContext.getPrincipal();
        Assertions.assertTrue(principalOpt.isPresent());
        Assertions.assertEquals("test-agent", principalOpt.get().identity());
        Assertions.assertTrue(SecurityContext.hasScope(SecurityConstants.SCOPE_READ));
        Assertions.assertTrue(SecurityContext.hasScope(SecurityConstants.SCOPE_TOOL_INVOKE));
        Assertions.assertTrue(SecurityContext.hasScope(SecurityConstants.SCOPE_WRITE_GRAPH));
    }

    @Test
    void testAuthenticateInvalidTokenThrows() {
        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            authenticationService.authenticate("invalid-token", "test-agent");
        });

        Assertions.assertEquals(SecurityConstants.ERROR_AUTHENTICATION_FAILED, exception.getErrorCode());
        Assertions.assertTrue(SecurityContext.getPrincipal().isEmpty());
    }

    @Test
    void testAuthenticateEmptyTokenThrows() {
        SecurityException exception = Assertions.assertThrows(SecurityException.class, () -> {
            authenticationService.authenticate("", "test-agent");
        });

        Assertions.assertEquals(SecurityConstants.ERROR_AUTHENTICATION_FAILED, exception.getErrorCode());
        Assertions.assertTrue(SecurityContext.getPrincipal().isEmpty());
    }
}
