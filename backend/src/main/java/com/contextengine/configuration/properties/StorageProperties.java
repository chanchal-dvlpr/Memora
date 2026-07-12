package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Storage-provider selection reserved for a future persistence implementation.
 *
 * @param provider the configured storage provider identifier
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.storage")
public record StorageProperties(
        @NotBlank @DefaultValue("unconfigured") String provider) {
}
