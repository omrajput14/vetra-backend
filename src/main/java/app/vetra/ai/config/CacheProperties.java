package app.vetra.ai.config;

import java.time.Duration;

/** Configuration properties for AI Gateway cache layer. */
public class CacheProperties {

  private boolean enabled = true;
  private Duration ttl = Duration.ofHours(24);
  private String keyPrefix = "vetra:ai:cache:";
  private Duration stampedeLockTimeout = Duration.ofSeconds(10);

  /** Default constructor. */
  public CacheProperties() {}

  /**
   * Returns true if cache layer is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets cache layer enabled status.
   *
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns cache entry TTL duration.
   *
   * @return TTL duration
   */
  public Duration getTtl() {
    return ttl;
  }

  /**
   * Sets cache entry TTL duration.
   *
   * @param ttl TTL duration
   */
  public void setTtl(Duration ttl) {
    this.ttl = ttl != null ? ttl : Duration.ofHours(24);
  }

  /**
   * Returns cache key prefix.
   *
   * @return key prefix string
   */
  public String getKeyPrefix() {
    return keyPrefix;
  }

  /**
   * Sets cache key prefix.
   *
   * @param keyPrefix key prefix string
   */
  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix != null ? keyPrefix : "vetra:ai:cache:";
  }

  /**
   * Returns cache stampede lock timeout.
   *
   * @return lock timeout duration
   */
  public Duration getStampedeLockTimeout() {
    return stampedeLockTimeout;
  }

  /**
   * Sets cache stampede lock timeout.
   *
   * @param stampedeLockTimeout lock timeout duration
   */
  public void setStampedeLockTimeout(Duration stampedeLockTimeout) {
    this.stampedeLockTimeout = stampedeLockTimeout != null ? stampedeLockTimeout : Duration.ofSeconds(10);
  }
}
