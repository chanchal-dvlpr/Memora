package com.contextengine.configuration.beans;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables shared application infrastructure without introducing application behavior.
 *
 * <p>Configuration properties are discovered from the dedicated properties package and validated
 * during startup. Asynchronous execution is enabled for future infrastructure tasks; scheduling is
 * intentionally not enabled because no scheduled architecture component exists yet.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = "com.contextengine.configuration.properties")
public class ApplicationConfiguration {
}
