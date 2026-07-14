package com.contextengine.mcp.resource;

import java.util.Objects;

/**
 * Represents the text content wrapper of an MCP resource.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Model Context Protocol Specification
 * Responsibility: Envelop resource data along with its source URI and format classification.
 * Dependencies: None.
 * Future Usage: Serialized inside resources/read JSON-RPC response lists.
 */
public record McpResourceContent(
    String uri,
    String mimeType,
    String text
) {
    public McpResourceContent {
        Objects.requireNonNull(uri, "Content URI must not be null");
        Objects.requireNonNull(text, "Content text must not be null");
    }
}
