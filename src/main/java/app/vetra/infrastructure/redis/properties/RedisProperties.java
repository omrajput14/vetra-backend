package app.vetra.infrastructure.redis.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly typed configuration properties for Vetra Redis infrastructure. Bound to prefix {@code
 * vetra.redis}.
 */
@ConfigurationProperties(prefix = "vetra.redis")
public class RedisProperties {

  private String host = "localhost";
  private int port = 6379;
  private String password = "";
  private int database = 0;
  private Duration timeout = Duration.ofMillis(2000);

  /**
   * Gets the Redis host name or IP address.
   *
   * @return Redis host name
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets the Redis host name or IP address.
   *
   * @param host Redis host name
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets the Redis port number.
   *
   * @return Redis port number
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the Redis port number.
   *
   * @param port Redis port number
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Gets the Redis authentication password.
   *
   * @return Redis password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the Redis authentication password.
   *
   * @param password Redis password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Gets the Redis logical database index.
   *
   * @return database index
   */
  public int getDatabase() {
    return database;
  }

  /**
   * Sets the Redis logical database index.
   *
   * @param database database index
   */
  public void setDatabase(int database) {
    this.database = database;
  }

  /**
   * Gets the command execution timeout duration.
   *
   * @return command timeout
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Sets the command execution timeout duration.
   *
   * @param timeout command timeout
   */
  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }
}
