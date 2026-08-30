package app.vetra.developer.dto;

/** Generic time-series data point for analytical charts. */
public record TimeSeriesPointDto(String date, long count) {}
