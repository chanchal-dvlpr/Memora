package com.contextengine.configuration.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the shared JSON mapper using Spring Boot's configured Jackson builder.
 *
 * <p>Using the Boot-provided builder preserves registered modules and externalized Jackson settings
 * while making the mapper available to future adapter implementations.</p>
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {

    private final Jackson2ObjectMapperBuilder objectMapperBuilder;

    /**
     * Creates the configuration with Spring Boot's configured Jackson builder.
     *
     * @param objectMapperBuilder the Boot-configured mapper builder
     */
    public JacksonConfiguration(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        this.objectMapperBuilder = objectMapperBuilder;
    }

    /**
     * Creates the shared JSON object mapper.
     *
     * @return an object mapper built from Spring Boot configuration
     */
    @Bean
    public ObjectMapper objectMapper() {
        return objectMapperBuilder.createXmlMapper(false).build();
    }
}
