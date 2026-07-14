package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Feature flags and credentials configuration for the Model Context Protocol (MCP) subsystem.
 *
 * @param enabled whether MCP capabilities are enabled
 * @param securityTokens list of authorized local API keys
 * @param stdioEnabled whether background standard I/O loop is active
 * @param httpEnabled whether loopback HTTP controller is active
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.mcp")
public record McpProperties(
    @NotNull @DefaultValue("false") Boolean enabled,
    List<String> securityTokens,
    @NotNull @DefaultValue("false") Boolean stdioEnabled,
    @NotNull @DefaultValue("true") Boolean httpEnabled
) {
}
