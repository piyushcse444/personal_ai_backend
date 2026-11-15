package com.personal_ai.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		final String securitySchemeName = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("Personal AI Backend API")
						.version("1.0.0")
						.description("REST API for Personal AI Backend application. Provides endpoints for user authentication, conversation management, provider configuration, and AI chat interactions.")
						.contact(new Contact()
								.name("Personal AI Team")
								.email("support@personal-ai.com")))
				.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
				.components(new io.swagger.v3.oas.models.Components()
						.addSecuritySchemes(securitySchemeName,
								new SecurityScheme()
										.name(securitySchemeName)
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Enter JWT Bearer token for authorization")));
	}
}
