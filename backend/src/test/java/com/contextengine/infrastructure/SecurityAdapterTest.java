package com.contextengine.infrastructure;

import com.contextengine.security.validation.SecurityInputValidator;
import com.contextengine.test.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies security adapters such as input validation and sanitization.
 */
class SecurityAdapterTest extends BaseUnitTest {

    @InjectMocks
    private SecurityInputValidator inputValidator;

    @Test
    void testInputValidatorSanitization() {
        String cleanHtml = inputValidator.sanitizeInputString("<script>alert('xss')</script>Hello World");
        assertThat(cleanHtml).isEqualTo("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;Hello World");

        assertThatThrownBy(() -> inputValidator.validateIdentifier("invalid-uuid"))
            .isInstanceOf(com.contextengine.security.foundation.SecurityException.class);
    }
}
