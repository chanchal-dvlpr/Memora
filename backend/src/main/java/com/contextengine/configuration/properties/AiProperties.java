package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flag reserved for future AI integrations.
 *
 * @param enabled whether AI integration capabilities are enabled
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.ai")
public record AiProperties(@NotNull @DefaultValue("false") Boolean enabled) {
}
