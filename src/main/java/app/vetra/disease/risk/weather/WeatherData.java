package app.vetra.disease.risk.weather;

/** DTO encapsulating regional meteorological context for disease vector & aerosol modeling. */
public record WeatherData(
    Double temperatureCelsius,
    Double relativeHumidityPercent,
    Double precipitationMm,
    boolean available,
    String statusDescription) {

  /** Factory method for successful weather reading. */
  public static WeatherData of(
      Double temp, Double humidity, Double precipitation, String description) {
    return new WeatherData(temp, humidity, precipitation, true, description);
  }

  /** Factory method when weather service is unreachable or coordinates are invalid. */
  public static WeatherData unavailable(String reason) {
    return new WeatherData(null, null, null, false, reason);
  }
}
