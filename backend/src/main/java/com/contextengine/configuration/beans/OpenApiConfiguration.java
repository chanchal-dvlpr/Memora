package com.contextengine.configuration.beans;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class provisioning the OpenAPI/Swagger generation framework beans.
 * <p>
 * Bounded Context: Platform Infrastructure
 * Reference: Section 9.8.1 (Documentation Completeness) PR-DOC-002
 * </p>
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * Instantiates the custom OpenAPI definition bean config.
     *
     * @return the configured OpenAPI definition
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Token Authentication";
        return new OpenAPI()
            .info(new Info()
                .title("Context Engine API")
                .version("1.0.0")
                .description("REST API specifications governing prompt context assembly, codebase index scans, and engineering metrics."))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Provide the ephemeral session bearer token passed in the Authorization header.")));
    }
}
