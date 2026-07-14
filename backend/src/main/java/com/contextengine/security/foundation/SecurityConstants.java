package com.contextengine.security.foundation;

/**
 * Shared security-specific constants and exception error codes for the Context Engine.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Section ID 4.4 (Security Constants)
 * Responsibility: Centralize scope permission strings and error status codes for authentication, authorization, and isolation.
 * Dependencies: None.
 * Future Usage: Referenced by security providers, validators, exception mappers, and unit tests to ensure parity.
 * </p>
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    /**
     * Standard scope for reading project metadata, resources, and templates.
     */
    public static final String SCOPE_READ = "mcp:read";

    /**
     * Standard scope for invoking operations and execute use cases.
     */
    public static final String SCOPE_TOOL_INVOKE = "mcp:tool_invoke";

    /**
     * Standard scope for writing knowledge graph elements or mutating project registries.
     */
    public static final String SCOPE_WRITE_GRAPH = "mcp:write_graph";

    /**
     * Standard error code for authentication failures.
     */
    public static final int ERROR_AUTHENTICATION_FAILED = 4001;

    /**
     * Standard error code for insufficient scope/permission privileges.
     */
    public static final int ERROR_SCOPE_INSUFFICIENT = 4002;

    /**
     * Standard error code for workspace locking or archived project rejection.
     */
    public static final int ERROR_WORKSPACE_LOCKED = -32001;

    /**
     * Standard error code for workspace isolation/directory traversal boundary breaches.
     */
    public static final int ERROR_BOUNDARY_VIOLATION = -32003;
}
