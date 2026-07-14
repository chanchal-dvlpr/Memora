package com.contextengine.mcp;

import java.util.Collections;
import java.util.Map;

/**
 * Base exception for all Model Context Protocol (MCP) protocol-level violations and tool errors.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 (Section 4.4 - Error Interface & Protocol Code Matrix)
 * Responsibility: Capture protocol-specific error state parameters including the integer error code
 * and execution context details, facilitating standardized response generation.
 * Dependencies: None.
 * Future Usage: Thrown by tool handlers, protocol parsers, and authorization guards to halt execution
 * and bubble standardized errors up to the JSON-RPC response mapper.
 */
public class McpException extends RuntimeException {

    private final int errorCode;
    private final Map<String, Object> details;

    /**
     * Constructs a new McpException with a message and error code.
     *
     * @param errorCode the JSON-RPC or protocol error code
     * @param message the descriptive error message
     */
    public McpException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    /**
     * Constructs a new McpException with a message, error code, and details.
     *
     * @param errorCode the JSON-RPC or protocol error code
     * @param message the descriptive error message
     * @param details validation or metadata failure details
     */
    public McpException(int errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? Map.copyOf(details) : Collections.emptyMap();
    }

    /**
     * Retrieves the protocol-level error code.
     *
     * @return the error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Retrieves validation or metadata failure details.
     *
     * @return the details map
     */
    public Map<String, Object> getDetails() {
        return details;
    }
}
