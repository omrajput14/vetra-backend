package app.vetra.ai.config;

import java.time.Duration;

/** Resilience and fault-tolerance configuration for a specific provider. */
public final class ResilienceConfig {

  private int maxRetryAttempts = 3;
  private Duration waitDuration = Duration.ofMillis(500);
  private double backoffMultiplier = 2.0;
  private float circuitBreakerFailureRateThreshold = 50.0f;
  private Duration circuitBreakerWaitDurationInOpenState = Duration.ofSeconds(10);
  private int circuitBreakerSlidingWindowSize = 10;
  private Duration timeout = Duration.ofSeconds(10);

  /** No-arg constructor. */
  public ResilienceConfig() {}

  /**
   * Returns the max retry attempts.
   *
   * @return retry count
   */
  public int getMaxRetryAttempts() {
    return maxRetryAttempts;
  }

  public void setMaxRetryAttempts(int maxRetryAttempts) {
    this.maxRetryAttempts = maxRetryAttempts;
  }

  /**
   * Returns initial retry wait duration.
   *
   * @return wait duration
   */
  public Duration getWaitDuration() {
    return waitDuration;
  }

  public void setWaitDuration(Duration waitDuration) {
    this.waitDuration = waitDuration;
  }

  /**
   * Returns exponential backoff multiplier.
   *
   * @return multiplier
   */
  public double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  public void setBackoffMultiplier(double backoffMultiplier) {
    this.backoffMultiplier = backoffMultiplier;
  }

  /**
   * Returns CircuitBreaker failure rate threshold percentage.
   *
   * @return failure rate threshold
   */
  public float getCircuitBreakerFailureRateThreshold() {
    return circuitBreakerFailureRateThreshold;
  }

  public void setCircuitBreakerFailureRateThreshold(float threshold) {
    this.circuitBreakerFailureRateThreshold = threshold;
  }

  /**
   * Returns duration CircuitBreaker stays OPEN before entering HALF_OPEN.
   *
   * @return open state wait duration
   */
  public Duration getCircuitBreakerWaitDurationInOpenState() {
    return circuitBreakerWaitDurationInOpenState;
  }

  public void setCircuitBreakerWaitDurationInOpenState(Duration duration) {
    this.circuitBreakerWaitDurationInOpenState = duration;
  }

  /**
   * Returns sliding window size for CircuitBreaker.
   *
   * @return window size
   */
  public int getCircuitBreakerSlidingWindowSize() {
    return circuitBreakerSlidingWindowSize;
  }

  public void setCircuitBreakerSlidingWindowSize(int size) {
    this.circuitBreakerSlidingWindowSize = size;
  }

  /**
   * Returns execution timeout duration.
   *
   * @return timeout duration
   */
  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }
}
