package com.contextengine.security;

import com.contextengine.security.foundation.SecurityContext;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SecurityContext}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Security Context Tests)
 * Purpose: Validate thread-local principal storage, scope matching, and context clear operations.
 * </p>
 */
class SecurityContextTest {

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void testSecurityContextEmptyByDefault() {
        Optional<SecurityContext.Principal> principalOpt = SecurityContext.getPrincipal();
        Assertions.assertTrue(principalOpt.isEmpty());
        Assertions.assertFalse(SecurityContext.hasScope("mcp:read"));
    }

    @Test
    void testSetAndGetPrincipal() {
        SecurityContext.Principal principal = new SecurityContext.Principal("test-agent", Set.of("mcp:read", "mcp:tool_invoke"));
        SecurityContext.setPrincipal(principal);

        Optional<SecurityContext.Principal> principalOpt = SecurityContext.getPrincipal();
        Assertions.assertTrue(principalOpt.isPresent());
        Assertions.assertEquals("test-agent", principalOpt.get().identity());
        Assertions.assertEquals(Set.of("mcp:read", "mcp:tool_invoke"), principalOpt.get().scopes());
    }

    @Test
    void testHasScope() {
        SecurityContext.Principal principal = new SecurityContext.Principal("test-agent", Set.of("mcp:read"));
        SecurityContext.setPrincipal(principal);

        Assertions.assertTrue(SecurityContext.hasScope("mcp:read"));
        Assertions.assertFalse(SecurityContext.hasScope("mcp:tool_invoke"));
    }

    @Test
    void testClearContext() {
        SecurityContext.Principal principal = new SecurityContext.Principal("test-agent", Set.of("mcp:read"));
        SecurityContext.setPrincipal(principal);
        Assertions.assertTrue(SecurityContext.getPrincipal().isPresent());

        SecurityContext.clear();
        Assertions.assertTrue(SecurityContext.getPrincipal().isEmpty());
    }
}
