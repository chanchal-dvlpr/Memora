package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flag reserved for the future knowledge-engine module.
 *
 * @param enabled whether knowledge-engine capabilities are enabled
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.knowledge")
public record KnowledgeProperties(@NotNull @DefaultValue("false") Boolean enabled) {
}
