package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Shared top-level settings for the Context Engine platform.
 *
 * @param mode the platform operating mode
 */
@Validated
@ConfigurationProperties(prefix = "context-engine")
public record ContextEngineProperties(
        @NotBlank @DefaultValue("local-first") String mode) {
}
