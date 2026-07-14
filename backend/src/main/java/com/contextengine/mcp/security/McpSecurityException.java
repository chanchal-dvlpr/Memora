package com.contextengine.mcp.security;

import com.contextengine.mcp.McpException;
import java.util.Map;

/**
 * Protocol exception thrown upon MCP security violations such as authentication failures or scope insufficiencies.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Error Code Matrix)
 * Responsibility: Capture security-specific protocol violation codes (4001, 4002) and detail maps.
 * Dependencies: {@link McpException}
 * Future Usage: Thrown by security interceptors or handlers during handshake validation.
 */
public class McpSecurityException extends McpException {

    /**
     * Constructs a McpSecurityException.
     *
     * @param errorCode the security error code
     * @param message description of the failure
     */
    public McpSecurityException(int errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a McpSecurityException with metadata details.
     *
     * @param errorCode the security error code
     * @param message description of the failure
     * @param details metadata mapping detailing the failure
     */
    public McpSecurityException(int errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
