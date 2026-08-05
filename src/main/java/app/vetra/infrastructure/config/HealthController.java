package app.vetra.infrastructure.config;

import app.vetra.infrastructure.response.ApiResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Infrastructure health and readiness endpoints.
 *
 * <p>These endpoints complement Spring Actuator and are designed to be polled by load balancers and
 * Kubernetes liveness/readiness probes without requiring authentication.
 */
@RestController
public class HealthController {

  /**
   * Application readiness probe.
   *
   * <p>Returns 200 OK when the Spring context has started and the application is ready to serve
   * traffic.
   *
   * @return readiness payload
   */
  @GetMapping("/readiness")
  public ApiResponse<Map<String, Object>> readiness() {
    return ApiResponse.ok(
        "Application is ready", Map.of("status", "READY", "timestamp", Instant.now().toString()));
  }

  /**
   * Application liveness probe.
   *
   * <p>Returns 200 OK as long as the JVM and Spring context are alive.
   *
   * @return liveness payload
   */
  @GetMapping("/liveness")
  public ApiResponse<Map<String, Object>> liveness() {
    return ApiResponse.ok(
        "Application is alive", Map.of("status", "ALIVE", "timestamp", Instant.now().toString()));
  }
}
