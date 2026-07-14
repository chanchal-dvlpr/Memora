package com.contextengine.mcp.prompt;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines a reusable prompt template exposed by the MCP Server.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Model Context Protocol Specification
 * Responsibility: Encapsulate template parameters, descriptors, and expected arguments.
 * Dependencies: {@link McpPromptArgument}
 * Future Usage: Serialized in prompts/list JSON-RPC responses.
 */
public record McpPrompt(
    String name,
    String description,
    List<McpPromptArgument> arguments
) {
    public McpPrompt {
        Objects.requireNonNull(name, "Prompt name must not be null");
        arguments = arguments != null ? List.copyOf(arguments) : Collections.emptyList();
    }
}
