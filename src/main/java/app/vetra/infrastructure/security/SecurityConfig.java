package app.vetra.infrastructure.security;

import app.vetra.infrastructure.config.CorsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <p>Configures a stateless JWT-secured filter chain with role-based access control. Explicitly
 * public paths: auth endpoints, Swagger UI, OpenAPI docs, and Actuator health.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  /** Public endpoints that do not require authentication. */
  private static final String[] PUBLIC_ENDPOINTS = {
    // Auth public endpoints
    "/api/v1/auth/farmer/register",
    "/api/v1/auth/farmer/login",
    "/api/v1/auth/vet/register",
    "/api/v1/auth/vet/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/logout",
    // OpenAPI / Swagger UI
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/v3/api-docs.yaml",
    // Actuator health / liveness / readiness only
    "/actuator/health",
    "/actuator/health/**",
    "/actuator/info",
    "/readiness",
    "/liveness",
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CorsConfig corsConfig;

  /** Constructor injection. */
  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
      CorsConfig corsConfig) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    this.corsConfig = corsConfig;
  }

  /**
   * Configures the main security filter chain.
   *
   * <ul>
   *   <li>CSRF disabled — stateless REST API using JWT
   *   <li>CORS sourced from {@link CorsConfig}
   *   <li>Session policy: STATELESS
   *   <li>CustomAuthenticationEntryPoint formats 401 unauthorized errors
   *   <li>JWT filter runs before {@link UsernamePasswordAuthenticationFilter}
   *   <li>All non-public endpoints require authentication
   * </ul>
   *
   * @param http Spring Security HTTP builder
   * @return configured {@link SecurityFilterChain}
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(eh ->
            eh.authenticationEntryPoint(customAuthenticationEntryPoint))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /**
   * BCrypt password encoder bean.
   *
   * <p>Strength 12 is used for a balance of security and performance on server hardware. This bean
   * will be used in the auth stage.
   *
   * @return {@link BCryptPasswordEncoder} with strength 12
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
