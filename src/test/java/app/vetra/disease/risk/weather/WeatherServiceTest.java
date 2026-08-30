package app.vetra.disease.risk.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.disease.risk.RiskScoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeatherServiceTest {

  private RiskScoreProperties properties;
  private ObjectMapper objectMapper;
  private HttpClient mockHttpClient;
  private HttpResponse<String> mockHttpResponse;
  private WeatherService weatherService;

  @BeforeEach
  void setUp() {
    properties = new RiskScoreProperties();
    properties.setWeatherApiBaseUrl("https://api.open-meteo.com/v1/forecast");
    properties.setWeatherTimeoutSeconds(3);
    properties.setWeatherConnectTimeoutSeconds(2);
    properties.setWeatherCacheTtlMinutes(30);
    properties.setWeatherEnabled(true);

    objectMapper = new ObjectMapper();
    mockHttpClient = mock(HttpClient.class);
    mockHttpResponse = mock(HttpResponse.class);

    weatherService = new WeatherService(properties, objectMapper, mockHttpClient);
  }

  @Test
  @DisplayName("1. Successful Open-Meteo mapping maps temperature, humidity, and precipitation")
  void testSuccessfulWeatherMapping() throws Exception {
    String sampleJson =
        """
        {
          "latitude": 18.52,
          "longitude": 73.86,
          "current": {
            "time": "2026-08-29T18:00",
            "temperature_2m": 24.5,
            "relative_humidity_2m": 82.0,
            "precipitation": 1.2
          }
        }
        """;

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(sampleJson);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockHttpResponse);

    WeatherData data = weatherService.getWeatherData(18.5204, 73.8567);

    assertNotNull(data);
    assertTrue(data.available());
    assertEquals(24.5, data.temperatureCelsius());
    assertEquals(82.0, data.relativeHumidityPercent());
    assertEquals(1.2, data.precipitationMm());
    assertTrue(data.statusDescription().contains("24.5°C"));
    assertTrue(data.statusDescription().contains("82%"));
    assertTrue(data.statusDescription().contains("1.2mm"));
  }

  @Test
  @DisplayName("2. Temperature mapping handles negative, zero, and high temperatures")
  void testTemperatureMapping_VariousValues() throws Exception {
    String sampleJson =
        """
        {
          "current": {
            "temperature_2m": -3.5,
            "relative_humidity_2m": 45.0,
            "precipitation": 0.0
          }
        }
        """;

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(sampleJson);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockHttpResponse);

    WeatherData data = weatherService.getWeatherData(34.0837, 74.7973); // Srinagar

    assertTrue(data.available());
    assertEquals(-3.5, data.temperatureCelsius());
    assertEquals(45.0, data.relativeHumidityPercent());
    assertEquals(0.0, data.precipitationMm());
  }

  @Test
  @DisplayName("3. Zero precipitation is accurately mapped and not treated as missing")
  void testPrecipitationZeroMapping() throws Exception {
    String sampleJson =
        """
        {
          "current": {
            "temperature_2m": 31.0,
            "relative_humidity_2m": 50.0,
            "precipitation": 0.0
          }
        }
        """;

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(sampleJson);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockHttpResponse);

    WeatherData data = weatherService.getWeatherData(19.0760, 72.8777); // Mumbai

    assertTrue(data.available());
    assertEquals(0.0, data.precipitationMm());
  }

  @Test
  @DisplayName("4. Invalid geographic coordinates return unavailable without making HTTP requests")
  void testInvalidCoordinates() throws Exception {
    WeatherData data1 = weatherService.getWeatherData(95.0, 73.0); // Lat > 90
    assertFalse(data1.available());
    assertNull(data1.temperatureCelsius());

    WeatherData data2 = weatherService.getWeatherData(18.0, 195.0); // Lng > 180
    assertFalse(data2.available());

    WeatherData data3 = weatherService.getWeatherData(Double.NaN, 73.0);
    assertFalse(data3.available());

    // Verify 0 network requests made
    verify(mockHttpClient, times(0)).send(any(), any());
  }

  @Test
  @DisplayName("5. API Timeout exception is caught safely and returns unavailable")
  void testApiTimeout() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new HttpTimeoutException("Connection timed out after 3000ms"));

    WeatherData data = weatherService.getWeatherData(18.5204, 73.8567);

    assertNotNull(data);
    assertFalse(data.available());
    assertNull(data.temperatureCelsius());
    assertTrue(data.statusDescription().contains("unavailable"));
  }

  @Test
  @DisplayName("6. Non-200 HTTP status returns unavailable gracefully")
  void testApiHttpError() throws Exception {
    when(mockHttpResponse.statusCode()).thenReturn(503);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockHttpResponse);

    WeatherData data = weatherService.getWeatherData(18.5204, 73.8567);

    assertNotNull(data);
    assertFalse(data.available());
    assertTrue(data.statusDescription().contains("503"));
  }

  @Test
  @DisplayName("7. Disabled weather service integration returns unavailable immediately")
  void testWeatherDisabled() throws Exception {
    properties.setWeatherEnabled(false);

    WeatherData data = weatherService.getWeatherData(18.5204, 73.8567);

    assertFalse(data.available());
    verify(mockHttpClient, times(0)).send(any(), any());
  }

  @Test
  @DisplayName("8. In-memory spatial cache serves repeated queries without network calls")
  void testCacheBehavior() throws Exception {
    String sampleJson =
        """
        {
          "current": {
            "temperature_2m": 26.0,
            "relative_humidity_2m": 70.0,
            "precipitation": 0.5
          }
        }
        """;

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(sampleJson);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockHttpResponse);

    // Call 1 -> Network call
    WeatherData data1 = weatherService.getWeatherData(18.5204, 73.8567);
    assertTrue(data1.available());
    assertEquals(1, weatherService.getCacheSize());

    // Call 2 -> Cache hit (same 2-decimal spatial key)
    WeatherData data2 = weatherService.getWeatherData(18.5204, 73.8567);
    assertTrue(data2.available());
    assertEquals(data1.temperatureCelsius(), data2.temperatureCelsius());

    // Verify only 1 network request was dispatched
    verify(mockHttpClient, times(1)).send(any(), any());

    // Clear cache
    weatherService.clearCache();
    assertEquals(0, weatherService.getCacheSize());
  }

  @Test
  @DisplayName("9. Live Open-Meteo integration test with real Maharashtra coordinates")
  void testLiveOpenMeteoCall() {
    WeatherService liveService = new WeatherService(properties, objectMapper);

    // Query Pune, Maharashtra (18.5204, 73.8567)
    WeatherData liveData = liveService.getWeatherData(18.5204, 73.8567);

    assertNotNull(liveData);
    assertNotNull(liveData.statusDescription());
    if (liveData.available()) {
      assertNotNull(liveData.temperatureCelsius(), "Real temperature should be returned");
      assertNotNull(liveData.relativeHumidityPercent(), "Real humidity should be returned");
      assertNotNull(liveData.precipitationMm(), "Real precipitation should be returned");
      assertTrue(liveData.temperatureCelsius() > -20.0 && liveData.temperatureCelsius() < 60.0);
      assertTrue(liveData.relativeHumidityPercent() >= 0.0 && liveData.relativeHumidityPercent() <= 100.0);
    }
  }
}
