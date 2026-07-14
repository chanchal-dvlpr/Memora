package com.contextengine.api;

/**
 * Defines the public REST API endpoints and path parameters.
 * All paths conform strictly to the version 1.0 specification guidelines.
 */
public final class ApiPaths {

    /**
     * API version 1 path segment prefix.
     */
    public static final String API_V1 = "/api/v1";

    /**
     * Endpoint path for projects management.
     */
    public static final String PROJECTS = API_V1 + "/projects";

    /**
     * Endpoint path for a specific project details by identifier.
     */
    public static final String PROJECTS_ID = PROJECTS + "/{id}";

    /**
     * Endpoint path for scanner controls.
     */
    public static final String SCANNERS = PROJECTS_ID + "/scanners";

    /**
     * Endpoint path for context assembly execution.
     */
    public static final String CONTEXT_ASSEMBLY = API_V1 + "/context/assembly";

    /**
     * Endpoint path for retrieving the latest compiled snapshot metadata.
     */
    public static final String SNAPSHOTS_LATEST = PROJECTS_ID + "/snapshots/latest";

    private ApiPaths() {
        // Prevent instantiation
    }
}
