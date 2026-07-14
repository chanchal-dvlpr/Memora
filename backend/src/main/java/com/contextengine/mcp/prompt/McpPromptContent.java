package com.contextengine.mcp.prompt;

import java.util.Objects;

/**
 * Standard protocol representation of content within an MCP prompt message (e.g. text blocks).
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Model Context Protocol Specification
 * Responsibility: Envelop text data with a standard type discriminator ("text").
 * Dependencies: None.
 * Future Usage: Included in McpPromptMessage list returned by prompts/get execution.
 */
public record McpPromptContent(
    String type,
    String text
) {
    public McpPromptContent {
        Objects.requireNonNull(type, "Content type must not be null");
        Objects.requireNonNull(text, "Content text must not be null");
    }

    /**
     * Helper to construct a standard text content block.
     *
     * @param text the message body text
     * @return a text typed McpPromptContent
     */
    public static McpPromptContent text(String text) {
        return new McpPromptContent("text", text);
    }
}
