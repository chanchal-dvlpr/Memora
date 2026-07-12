package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flag reserved for future search capabilities.
 *
 * @param enabled whether search capabilities are enabled
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.search")
public record SearchProperties(@NotNull @DefaultValue("false") Boolean enabled) {
}
