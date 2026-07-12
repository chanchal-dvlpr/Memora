package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Workspace location settings reserved for future workspace-aware modules.
 *
 * @param root the configured workspace root path
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.workspace")
public record WorkspaceProperties(
        @NotBlank @DefaultValue("${user.home}/.context-engine/workspaces") String root) {
}
