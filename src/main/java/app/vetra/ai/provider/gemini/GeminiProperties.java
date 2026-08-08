package app.vetra.ai.provider.gemini;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Externalized configuration properties for Gemini Vision AI Provider integration. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vetra.ai.gemini")
public class GeminiProperties {

  /** Enables or disables the Gemini AI provider strategy. */
  private boolean enabled = false;

  /** Gemini REST API key loaded securely via environment variable. */
  private String apiKey = "";

  /** Gemini Vision model identifier (e.g. gemini-1.5-flash, gemini-2.0-flash). */
  private String model = "gemini-1.5-flash";

  /** Google Generative Language Base API URL. */
  private String baseUrl = "https://generativelanguage.googleapis.com";

  /** WebClient execution timeout. */
  private Duration timeout = Duration.ofSeconds(30);

  /** Max HTTP request retry attempts. */
  private int maxRetries = 3;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }
}
