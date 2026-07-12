package com.contextengine.configuration.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Provides a named conversion service for future adapter and configuration conversions.
 *
 * <p>The bean is deliberately named so it does not replace Spring Boot's application conversion
 * service used for framework binding.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ConversionConfiguration {

    /**
     * Creates a reusable formatting-aware conversion service.
     *
     * @return the named Context Engine conversion service
     */
    @Bean("contextEngineConversionService")
    public ConversionService contextEngineConversionService() {
        return new DefaultFormattingConversionService();
    }
}
