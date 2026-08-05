package app.vetra.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration properties.
 *
 * <p>Bound from {@code vetra.jwt.*} in application.yml. Implementation will be added in the auth
 * stage.
 */
@Validated
@ConfigurationProperties(prefix = "vetra.jwt")
public record JwtProperties(
    @NotBlank String secret, @Positive long expirationMs, @Positive long refreshExpirationMs) {}
