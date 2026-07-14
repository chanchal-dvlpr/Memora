package com.contextengine.mcp.prompt;

import java.util.Objects;

/**
 * Defines a single input parameter required or accepted by a prompt template.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / Model Context Protocol Specification
 * Responsibility: Capture the name, description, and requirement flag of a prompt argument.
 * Dependencies: None.
 * Future Usage: Included in the metadata list of an exposed McpPrompt.
 */
public record McpPromptArgument(
    String name,
    String description,
    boolean required
) {
    public McpPromptArgument {
        Objects.requireNonNull(name, "Argument name must not be null");
    }
}
