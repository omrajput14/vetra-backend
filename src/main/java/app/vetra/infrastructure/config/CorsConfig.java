package app.vetra.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration.
 *
 * <p>Provides a {@link CorsConfigurationSource} bean consumed by {@link SecurityConfig}. Allowed
 * origins are loaded from {@code vetra.cors.allowed-origins} which supports a comma-separated list.
 */
@Configuration
public class CorsConfig {

  private final CorsProperties corsProperties;

  /** Constructor injection. */
  public CorsConfig(CorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  /**
   * Constructs a CORS configuration source from application properties.
   *
   * @return configured {@link CorsConfigurationSource}
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOrigins(corsProperties.allowedOrigins());
    config.setAllowedMethods(Arrays.asList(corsProperties.allowedMethods().split(",")));
    config.setAllowedHeaders(List.of(corsProperties.allowedHeaders()));
    config.setAllowCredentials(corsProperties.allowCredentials());
    config.setMaxAge(corsProperties.maxAge());

    // Expose headers the client SDK may need to read
    config.setExposedHeaders(List.of("Authorization", "X-Request-Id", "X-Total-Count", "Location"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
