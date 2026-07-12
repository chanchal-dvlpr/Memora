package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration identifying the current Context Engine project.
 *
 * @param name the stable project name used by future module configuration
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.project")
public record ProjectProperties(
        @NotBlank @DefaultValue("context-engine-backend") String name) {
}
