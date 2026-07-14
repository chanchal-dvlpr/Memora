package com.contextengine.security.foundation;

import java.util.Collections;
import java.util.Map;

/**
 * Shared runtime exception thrown upon security violations in the authentication, authorization, or workspace isolation layers.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Common Security Exceptions)
 * Responsibility: Capture security-specific error codes and metadata details to facilitate clean response formatting.
 * Dependencies: None.
 * Future Usage: Caught by protocol handlers or MVC exception interceptors to output structured JSON-RPC / REST error payloads.
 * </p>
 */
public class SecurityException extends RuntimeException {

    private final int errorCode;
    private final Map<String, Object> details;

    /**
     * Constructs a SecurityException with an error code and message.
     *
     * @param errorCode the security error code
     * @param message the descriptive error message
     */
    public SecurityException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    /**
     * Constructs a SecurityException with an error code, message, and details metadata.
     *
     * @param errorCode the security error code
     * @param message the descriptive error message
     * @param details detailed security violation attributes
     */
    public SecurityException(int errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? Map.copyOf(details) : Collections.emptyMap();
    }

    /**
     * Returns the security error code.
     *
     * @return error code integer
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the validation or metadata details associated with this failure.
     *
     * @return unmodifiable map of violation details
     */
    public Map<String, Object> getDetails() {
        return details;
    }
}
