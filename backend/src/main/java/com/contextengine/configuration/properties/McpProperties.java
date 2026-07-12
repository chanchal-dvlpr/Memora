package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flag reserved for the future Model Context Protocol adapter.
 *
 * @param enabled whether MCP capabilities are enabled
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.mcp")
public record McpProperties(@NotNull @DefaultValue("false") Boolean enabled) {
}
