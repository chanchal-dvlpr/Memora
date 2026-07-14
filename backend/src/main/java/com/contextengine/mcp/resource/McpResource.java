package com.contextengine.mcp.resource;

import java.util.Objects;

/**
 * Represents metadata of an addressable resource exposed by the MCP Server.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Model Context Protocol Specification
 * Responsibility: Hold standard properties describing a resource (URI, name, description, mimeType).
 * Dependencies: None.
 * Future Usage: Serialized inside resources/list JSON-RPC responses to client applications.
 */
public record McpResource(
    String uri,
    String name,
    String description,
    String mimeType
) {
    public McpResource {
        Objects.requireNonNull(uri, "Resource URI must not be null");
        Objects.requireNonNull(name, "Resource name must not be null");
    }
}
