package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flag reserved for future graph capabilities.
 *
 * @param enabled whether graph capabilities are enabled
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.graph")
public record GraphProperties(@NotNull @DefaultValue("false") Boolean enabled) {
}
