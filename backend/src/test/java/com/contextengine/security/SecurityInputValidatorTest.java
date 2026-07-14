package com.contextengine.security;

import com.contextengine.security.foundation.SecurityException;
import com.contextengine.security.validation.SecurityInputValidator;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SecurityInputValidator}.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Input Validation Tests)
 * Purpose: Validate UUID patterns, block path traversals, and verify string sanitizations.
 * </p>
 */
class SecurityInputValidatorTest {

    private SecurityInputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SecurityInputValidator();
    }

    @Test
    void testValidateIdentifierSuccess() {
        String validUuid = UUID.randomUUID().toString();
        Assertions.assertDoesNotThrow(() -> {
            validator.validateIdentifier(validUuid);
        });
    }

    @Test
    void testValidateIdentifierFailure() {
        Assertions.assertThrows(SecurityException.class, () -> {
            validator.validateIdentifier("invalid-uuid-1234");
        });
        Assertions.assertThrows(SecurityException.class, () -> {
            validator.validateIdentifier(null);
        });
    }

    @Test
    void testValidatePathStringSuccess() {
        Assertions.assertDoesNotThrow(() -> {
            validator.validatePathString("/Users/username/workspace/project/src/main.java");
        });
    }

    @Test
    void testValidatePathStringTraversalThrows() {
        Assertions.assertThrows(SecurityException.class, () -> {
            validator.validatePathString("/Users/username/workspace/project/../outside.java");
        });
        Assertions.assertThrows(SecurityException.class, () -> {
            validator.validatePathString("..");
        });
    }

    @Test
    void testSanitizeInputString() {
        String input = "<script>alert('hack');</script>";
        String expected = "&lt;script&gt;alert(&#x27;hack&#x27;);&lt;/script&gt;";
        Assertions.assertEquals(expected, validator.sanitizeInputString(input));

        Assertions.assertNull(validator.sanitizeInputString(null));
    }
}
