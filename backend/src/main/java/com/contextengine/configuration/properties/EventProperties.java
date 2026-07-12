package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flag reserved for future event infrastructure.
 *
 * @param enabled whether event infrastructure is enabled
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.events")
public record EventProperties(@NotNull @DefaultValue("false") Boolean enabled) {
}
