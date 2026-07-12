package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cache-provider selection reserved for a future cache implementation.
 *
 * @param provider the configured cache provider identifier
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.cache")
public record CacheProperties(@NotBlank @DefaultValue("none") String provider) {
}
