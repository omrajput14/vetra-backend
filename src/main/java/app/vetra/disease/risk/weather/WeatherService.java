package app.vetra.disease.risk.weather;

import app.vetra.disease.risk.RiskScoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * High-performance meteorological service integrating Open-Meteo REST API.
 * Features in-memory spatial caching and non-blocking circuit fallback.
 */
@Service
public class WeatherService {

  private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

  private final RiskScoreProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();

  /** Spring constructor injection. */
  @Autowired
  public WeatherService(RiskScoreProperties properties, ObjectMapper objectMapper) {
    this(
        properties,
        objectMapper,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getWeatherConnectTimeoutSeconds()))
            .build());
  }

  /** Package-private constructor for dependency injection in testing. */
  WeatherService(
      RiskScoreProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  /**
   * Retrieves current weather parameters for the given geographic coordinate.
   *
   * @param latitude latitude in decimal degrees
   * @param longitude longitude in decimal degrees
   * @return {@link WeatherData} instance (never null)
   */
  public WeatherData getWeatherData(double latitude, double longitude) {
    if (Double.isNaN(latitude)
        || Double.isNaN(longitude)
        || latitude < -90.0
        || latitude > 90.0
        || longitude < -180.0
        || longitude > 180.0) {
      return WeatherData.unavailable(
          String.format(Locale.ROOT, "Invalid geographic coordinates: Lat %.4f, Lng %.4f", latitude, longitude));
    }

    if (!properties.isWeatherEnabled()) {
      return WeatherData.unavailable("Environmental weather data integration is disabled");
    }

    String cacheKey = String.format(Locale.ROOT, "%.2f:%.2f", latitude, longitude);
    Instant now = Instant.now();

    CacheEntry cached = weatherCache.get(cacheKey);
    if (cached != null
        && cached.timestamp.plus(Duration.ofMinutes(properties.getWeatherCacheTtlMinutes())).isAfter(now)) {
      return cached.data;
    }

    try {
      String url =
          String.format(
              Locale.ROOT,
              "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,precipitation",
              properties.getWeatherApiBaseUrl(),
              latitude,
              longitude);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(properties.getWeatherTimeoutSeconds()))
              .header("User-Agent", "VETRA-Surveillance-Engine/1.0")
              .GET()
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        WeatherData data = parseOpenMeteoResponse(response.body());
        if (data.available()) {
          weatherCache.put(cacheKey, new CacheEntry(data, now));
        }
        return data;
      }

      log.warn("Open-Meteo returned HTTP {}", response.statusCode());
      return WeatherData.unavailable("Weather service returned status " + response.statusCode());
    } catch (Exception e) {
      log.debug("Weather API call failed gracefully: {}", e.getMessage());
      return WeatherData.unavailable("Environmental weather data temporarily unavailable");
    }
  }

  private WeatherData parseOpenMeteoResponse(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode current = root.path("current");

      if (current.isMissingNode() || current.isNull()) {
        return WeatherData.unavailable("Open-Meteo response missing current weather data block");
      }

      Double temp =
          current.hasNonNull("temperature_2m")
              ? current.path("temperature_2m").asDouble()
              : null;
      Double humidity =
          current.hasNonNull("relative_humidity_2m")
              ? current.path("relative_humidity_2m").asDouble()
              : null;
      Double precipitation =
          current.hasNonNull("precipitation")
              ? current.path("precipitation").asDouble()
              : null;

      if (temp == null && humidity == null) {
        return WeatherData.unavailable("Open-Meteo response missing essential meteorological parameters");
      }

      String desc =
          String.format(
              Locale.ROOT,
              "Temp: %s, Humidity: %s, Rain: %s",
              temp != null ? String.format(Locale.ROOT, "%.1f°C", temp) : "N/A",
              humidity != null ? String.format(Locale.ROOT, "%.0f%%", humidity) : "N/A",
              precipitation != null ? String.format(Locale.ROOT, "%.1fmm", precipitation) : "N/A");

      return WeatherData.of(temp, humidity, precipitation, desc);
    } catch (Exception e) {
      log.warn("Failed to parse Open-Meteo response JSON: {}", e.getMessage());
      return WeatherData.unavailable("Failed to parse weather service response");
    }
  }

  /** Clears the spatial weather cache. */
  public void clearCache() {
    weatherCache.clear();
  }

  /** Returns the current size of the spatial weather cache. */
  public int getCacheSize() {
    return weatherCache.size();
  }

  private record CacheEntry(WeatherData data, Instant timestamp) {}
}
