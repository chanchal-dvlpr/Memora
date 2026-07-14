package com.contextengine.mcp.prompt;

import java.util.Objects;

/**
 * Standard protocol representation of a chat message within an MCP prompt response.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Model Context Protocol Specification
 * Responsibility: Wrap role (user/assistant) and structured content items.
 * Dependencies: {@link McpPromptContent}
 * Future Usage: Serialized inside prompts/get response results.
 */
public record McpPromptMessage(
    String role,
    McpPromptContent content
) {
    public McpPromptMessage {
        Objects.requireNonNull(role, "Message role must not be null");
        Objects.requireNonNull(content, "Message content must not be null");
    }
}
