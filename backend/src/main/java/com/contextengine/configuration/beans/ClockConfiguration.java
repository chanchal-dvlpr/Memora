package com.contextengine.configuration.beans;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the authoritative clock for time-aware infrastructure and future use cases.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    /**
     * Creates a UTC clock so time handling is independent of the host timezone.
     *
     * @return the shared UTC clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
