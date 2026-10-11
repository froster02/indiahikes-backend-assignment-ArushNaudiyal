package com.indiahikes.trekops.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui/index.html. Log in with POST /api/auth/login, then paste the token into
 * "Authorize" and every protected call sends it as a Bearer header.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  public OpenAPI trekOpsOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Trek Operations API")
                .version("v1")
                .description(
                    "Treks, checkpoints, participants, check-ins and delay alerts. "
                        + "Seed users: admin / Admin@12345 (ADMIN), ops / Ops@12345 (OPERATIONS)."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }
}
