package app.vetra.ai.config;

import app.vetra.ai.entity.AIProviderType;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Externalized configuration properties for AI platform orchestration. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vetra.ai")
public class AIProperties {

  /** Master switch enabling AI diagnostic processing platform. */
  private boolean enabled = false;

  /** Default AI provider type to fallback or route requests to. */
  private AIProviderType defaultProvider = AIProviderType.NONE;

  /** Request execution timeout duration. */
  private Duration timeout = Duration.ofSeconds(30);

  /** Retry policy configurations. */
  private RetryProperties retry = new RetryProperties();

  /** Nested retry policy properties. */
  @Getter
  @Setter
  public static class RetryProperties {
    private int maxAttempts = 3;
    private Duration backoff = Duration.ofSeconds(2);
  }
}
