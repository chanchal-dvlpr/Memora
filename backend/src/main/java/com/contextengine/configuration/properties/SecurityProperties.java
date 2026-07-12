package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Security-mode selection reserved for future security architecture.
 *
 * @param mode the configured security mode identifier
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.security")
public record SecurityProperties(@NotBlank @DefaultValue("unconfigured") String mode) {
}
