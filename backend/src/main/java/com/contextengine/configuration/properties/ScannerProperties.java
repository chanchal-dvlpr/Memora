package com.contextengine.configuration.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Feature flags reserved for the future source scanner module.
 *
 * @param enabled whether scanner capabilities are enabled
 * @param watchEnabled whether workspace watching is enabled
 */
@Validated
@ConfigurationProperties(prefix = "context-engine.scanner")
public record ScannerProperties(
        @NotNull @DefaultValue("false") Boolean enabled,
        @NotNull @DefaultValue("false") Boolean watchEnabled) {
}
