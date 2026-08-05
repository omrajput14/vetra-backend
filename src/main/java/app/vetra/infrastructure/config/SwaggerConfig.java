package app.vetra.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI / Swagger UI configuration.
 *
 * <p>Registers a JWT Bearer security scheme globally. The Swagger UI is only available in the
 * {@code dev} profile (disabled in {@code prod} via application-prod.yml).
 */
@Configuration
public class SwaggerConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  private final String appVersion;

  /** Constructor injection for the project version from POM. */
  public SwaggerConfig(@Value("${spring.application.name:vetra-backend}") String appName) {
    this.appVersion = appName;
  }

  /**
   * Configures the OpenAPI specification with Vetra branding, JWT security scheme, and server
   * entries.
   *
   * @return fully configured {@link OpenAPI} instance
   */
  @Bean
  public OpenAPI vetraOpenApi() {
    SecurityScheme bearerScheme =
        new SecurityScheme()
            .name(BEARER_SCHEME)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description(
                "Provide the JWT token obtained from /api/v1/auth/login. "
                    + "Format: Bearer {token}");

    return new OpenAPI()
        .info(apiInfo())
        .servers(
            List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://api.vetra.app").description("Production")))
        .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }

  private Info apiInfo() {
    return new Info()
        .title("Vetra API")
        .version("v1")
        .description(
            "Production REST API for the Vetra Livestock and Veterinary Healthcare Platform. "
                + "Roles: FARMER, VETERINARIAN, ADMINISTRATOR.")
        .contact(
            new Contact()
                .name("Vetra Engineering")
                .email("backend@vetra.app")
                .url("https://github.com/omrajput14/vetra"))
        .license(new License().name("Proprietary").url("https://vetra.app/terms"));
  }
}
