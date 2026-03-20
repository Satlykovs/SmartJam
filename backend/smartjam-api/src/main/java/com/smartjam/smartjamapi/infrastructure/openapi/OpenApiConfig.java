package com.smartjam.smartjamapi.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI documentation using SpringDoc. Defines metadata, security schemes, and global requirements
 * for the API.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    /**
     * Customizes the OpenAPI definition.
     *
     * @return a fully configured OpenAPI object.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, createSecurityScheme()));
    }

    private Info createApiInfo() {
        return new Info()
                .title("SmartJam API")
                .version("1.0")
                .description("""
                    Interactive Music Learning and Performance Analysis System.

                    Development Team:
                    - Sanjar Satlykov ([satlykovs@gmail.com](mailto:satlykovs@gmail.com))
                    - Anton Podrezov ([toni.podrezov@gmail.com](mailto:toni.podrezov@gmail.com))
                    - Serj Baskov ([baskovs450@gmail.com](mailto:baskovs450@gmail.com))

                    Supervised by:
                    - Andrey Sheremeev ([sheremeev.andrey@gmail.com](mailto:sheremeev.andrey@gmail.com))
                    """)
                .contact(new Contact()
                        .name("SmartJam Team")
                        .email("satlykovs@gmail.com")
                        .url("https://github.com/Satlykovs/SmartJam"));
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide your JWT token obtained from the login endpoint.");
    }
}
