package com.contextengine.configuration.beans;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Connects Jakarta Validation to the application's message source.
 *
 * <p>This allows future DTO and configuration-property constraints to resolve messages through the
 * standard {@code ValidationMessages.properties} bundle without requiring per-module setup.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ValidationConfiguration {

    private final MessageSource messageSource;

    /**
     * Creates the configuration with the application message source.
     *
     * @param messageSource the configured message source
     */
    public ValidationConfiguration(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Creates the shared Jakarta Validation factory.
     *
     * @return the validation factory backed by the application message source
     */
    @Bean
    public LocalValidatorFactoryBean defaultValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }
}
